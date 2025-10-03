package org.jahia.se.modules.anthropic.service;

import org.jahia.services.tags.TaggingService;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import javax.jcr.RepositoryException;

import org.jahia.api.Constants;
import org.jahia.se.modules.anthropic.service.AnthropicService;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRPropertyWrapperImpl;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRTemplate;
import org.jahia.services.content.LazyPropertyIterator;
import org.jahia.services.content.nodetypes.ExtendedPropertyDefinition;
import org.jahia.utils.LanguageCodeConverters;
import org.jahia.services.tags.TaggingService;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.ContentBlock;

@Component(service = { AnthropicService.class,
        ManagedService.class }, property = "service.pid=org.jahia.se.modules.anthropic", immediate = true)
public class AnthropicServiceImpl implements AnthropicService, ManagedService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnthropicServiceImpl.class);

    public static String api_key;
    public static String api_base_url;
    public static String model;
    public static String max_tokens;
    public static String prompt;

    private TaggingService taggingService;


    private Map<String, String> targetLanguages;

    @Reference(service = TaggingService.class, cardinality = ReferenceCardinality.MANDATORY)
    public void setTaggingService(TaggingService taggingService) {
        this.taggingService = taggingService;
    }

    @Override
    public List<String> generateAutoTags(String path, String language, String tagLanguage) {
        String cleanText = getTextFromNode(path, language);
        if (cleanText == null || cleanText.isEmpty()) {
            return Collections.emptyList();
        }
        final Locale srcLocale = LanguageCodeConverters.getLocaleFromCode(language);

        JCRNodeWrapper node = null;
        try {
            node = JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(
                    null, Constants.EDIT_WORKSPACE, srcLocale, (JCRSessionWrapper session) -> session.getNode(path)
            );
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
        String finalPrompt = (prompt != null && !prompt.isEmpty() ? prompt : "Text:") + " in " + tagLanguage + ": " + cleanText;
        LOGGER.info("Using prompt: {} - for text : {}", finalPrompt, cleanText);
        return invokeAnthropic(node, cleanText, tagLanguage);
    }

    public static String getTextFromNode(String path, String language) {
        try {
            final Locale srcLocale = LanguageCodeConverters.getLocaleFromCode(language);
            final Map<String, String> contentToAnalyse = JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(
                    null, Constants.EDIT_WORKSPACE, srcLocale, (JCRSessionWrapper session) -> {
                        Map<String, String> srcValues = new HashMap<>();
                        LazyPropertyIterator propertyIterator = (LazyPropertyIterator) session.getNode(path).getProperties();
                        while (propertyIterator.hasNext()) {
                            JCRPropertyWrapperImpl property = (JCRPropertyWrapperImpl) propertyIterator.nextProperty();
                            if (property.getDefinition().isInternationalized() && !property.getDefinition().isMultiple()) {
                                srcValues.put(property.getName(), property.getValue().getString());
                            }
                        }
                        return srcValues;
                    }
            );
            StringBuilder textBuilder = new StringBuilder();
            for (Map.Entry<String, String> entry : contentToAnalyse.entrySet()) {
                textBuilder.append(entry.getValue()).append(" ");
            }
            return removeHtmlTags(textBuilder.toString()).trim();
        } catch (RepositoryException ex) {
            LOGGER.error("Impossible to extract text from node: {} in {}", path, language, ex);
            return null;
        }
    }

    private static String removeHtmlTags(String inputText) {
        Document doc = Jsoup.parse(inputText);
        return doc.text();
    }

    public static String limitStringLength(String input, int maxLength) {
        if (input == null) return null;

        int actualLength = input.length();
        if (actualLength > maxLength) {
            LOGGER.debug("Truncating input from {} to {} characters", actualLength, maxLength);
            return input.substring(0, maxLength);
        }

        return input;
    }

    @Override
    public void updated(Dictionary<String, ?> dictionary) throws ConfigurationException {
        if (dictionary != null) {
            api_key = (String) dictionary.get("ANTHROPIC_API_KEY");
            api_base_url = (String) dictionary.get("ANTHROPIC_API_BASE_URL");
            model = (String) dictionary.get("ANTHROPIC_MODEL");
            max_tokens = (String) dictionary.get("ANTHROPIC_MAX_TOKENS");
            prompt = (String) dictionary.get("ANTHROPIC_USER_PROMPT");
        }
        if (api_key == null || api_key.trim().isEmpty()) {
            LOGGER.error("Anthropic API key not defined. Please configure ANTHROPIC_API_KEY.");
        } else {
            LOGGER.debug("Anthropic API configured. baseUrl={} model={}", api_base_url, model);
        }
    }

    public static String encode(String input) {
        if (input == null) {
            return null;
        }
        StringBuilder encoded = new StringBuilder();
        for (char c : input.toCharArray()) {
            switch (c) {
                case '&':
                    encoded.append("&amp;");
                    break;
                case '<':
                    encoded.append("&lt;");
                    break;
                case '>':
                    encoded.append("&gt;");
                    break;
                case '"':
                    encoded.append("&quot;");
                    break;
                case '\'':
                    encoded.append("&#39;");
                    break;
                case '/':
                    encoded.append("&#x2F;");
                    break;
                default:
                    encoded.append(c);
                    break;
            }
        }
        return encoded.toString();
    }

    public List<String> invokeAnthropic(JCRNodeWrapper node, String text, String tagLanguage) {
        if (api_key == null || api_key.isEmpty()) {
            LOGGER.error("Anthropic API key is not configured");
            return Collections.emptyList();
        }
        if (text == null || text.isEmpty()) {
            LOGGER.warn("No text provided to invokeAnthropic");
            return Collections.emptyList();
        }

        try {
            AnthropicClient client = AnthropicOkHttpClient.builder()
                    .apiKey(api_key)
                    .build();

            text = removeHtmlTags(text);
            text = encode(text);

            String promptText = (prompt != null && !prompt.isEmpty() ? prompt : "Text:") + " in " + tagLanguage + ": " + text;

            MessageCreateParams createParams = MessageCreateParams.builder()
                    .model(model != null ? Model.of(model) : Model.CLAUDE_SONNET_4_20250514)
                    .maxTokens(max_tokens != null ? Integer.parseInt(max_tokens) : 256)
                    .addUserMessage(promptText)
                    .build();

            Message message = client.messages().create(createParams);

            StringBuilder resultBuilder = new StringBuilder();
            for (ContentBlock block : message.content()) {
                Optional<com.anthropic.models.messages.TextBlock> tbOpt = block.text();
                tbOpt.ifPresent(tb -> resultBuilder.append(tb.text()));
            }

            String resultText = resultBuilder.toString();
            LOGGER.info("Anthropic response: {}", resultText);
            // Remove markdown-style code block markers (``` and ```json)
            String cleaned = resultText.replaceAll("(?s)```(?:json)?\\s*", "").replaceAll("\\s*```", "").trim();
            List<String> tags = new ArrayList<>();

            try {
                JSONArray arr = new JSONArray(cleaned);
                for (int i = 0; i < arr.length(); i++) {
                    String t = arr.optString(i, "").trim();
                    if (!t.isEmpty()) tags.add(t);
                }
            } catch (JSONException e) {
                LOGGER.warn("Failed to parse JSON array, fallback to CSV parsing");
                for (String tag : cleaned.split(",")) {
                    String t = tag.replaceAll("^\"|\"$", "").trim();
                    if (!t.isEmpty() && !"[".equals(t) && !"]".equals(t)) {
                        tags.add(t);
                    }
                }
            }
            LOGGER.info("Extracted tags: {}", tags);
            /*if (!tags.isEmpty()) {
                taggingService.tag(node, tags);
            }*/
            return tags;

        } catch (Exception e) {
            LOGGER.error("Erreur lors de l'appel à l'SDK Anthropic", e);
            return Collections.emptyList();
        }
    }
}
