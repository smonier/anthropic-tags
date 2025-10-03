package org.jahia.se.modules.anthropic.actions;

import java.util.List;
import java.util.Map;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.se.modules.anthropic.service.AnthropicService;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = Action.class, immediate = true)
public class AnthropicAction extends Action {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnthropicAction.class);

    @Activate
    public void activate() {
        setName("anthropicAction");
        setRequireAuthenticatedUser(true);
        setRequiredPermission("jcr:write_default");
        setRequiredWorkspace("default");
        setRequiredMethods("GET,POST");
    }

    private AnthropicService anthropicService;

    @Reference(service = AnthropicService.class)
    public void setAnthropicService(AnthropicService anthropicService) {
        this.anthropicService = anthropicService;
    }

    public AnthropicService getAnthropicService() {
        return anthropicService;
    }

    @Override
    public ActionResult doExecute(final HttpServletRequest request, final RenderContext renderContext, final Resource resource, final JCRSessionWrapper session, Map<String, List<String>> parameters, final URLResolver urlResolver) throws Exception {
        JSONObject resp = new JSONObject();
        int resultCode = HttpServletResponse.SC_BAD_REQUEST;

        String tagLanguage = "";
        if (parameters.containsKey("tagLanguage") && !parameters.get("tagLanguage").isEmpty()) {
            tagLanguage = parameters.get("tagLanguage").get(0);
        }

        try {
            resp = executePrompt(resource.getNode(), resource, tagLanguage);
            resultCode = HttpServletResponse.SC_OK;
        } catch (Exception e) {
            LOGGER.error("Error Accessing Anthropic: ", e);
            resp.put("error", e.getMessage());
        }

        LOGGER.info(resp.toString());
        return new ActionResult(resultCode, null, resp);
    }

    private JSONObject executePrompt(JCRNodeWrapper node, Resource resource, String tagLanguage) throws RepositoryException, JSONException {
        JSONObject resp = new JSONObject();
        // Utilise la méthode de génération de tags par Anthropic
        String currentLanguage = resource.getLocale().getLanguage();

        List<String> tags = anthropicService.generateAutoTags(node.getPath(), currentLanguage, tagLanguage);
        resp.put("tags", tags);
        resp.put("resultCode", HttpServletResponse.SC_OK);
        return resp;
    }
}
