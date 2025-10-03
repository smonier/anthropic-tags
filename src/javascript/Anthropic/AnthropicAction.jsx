import React, {useContext} from 'react';
import PropTypes from 'prop-types';
import {AnthropicDialog} from './AnthropicDialog';
import {getFullLanguageName} from './Anthropic.utils';
import {ComponentRendererContext} from '@jahia/ui-extender';
import {useFormikContext} from 'formik';
import {useContentEditorContext} from '@jahia/jcontent';

export const AnthropicActionComponent = ({render: Render, ...otherProps}) => {
    const {render, destroy} = useContext(ComponentRendererContext);
    const formik = useFormikContext();
    const {nodeData, lang, siteInfo} = useContentEditorContext();

    return (
        <Render {...otherProps}
                enabled={siteInfo.languages.length >= 1 && nodeData.hasWritePermission}
                onClick={() => {
                    render('AnthropicDialog', AnthropicDialog, {
                        isOpen: true,
                        path: nodeData.path,
                        uuid: nodeData.uuid,
                        formik,
                        language: getFullLanguageName(siteInfo.languages, lang),
                        langLocale: lang,
                        availableLanguages: siteInfo.languages,
                        onCloseDialog: () => destroy('AnthropicDialog')
                    });
                }}/>
    );
};

AnthropicActionComponent.propTypes = {
    render: PropTypes.func.isRequired
};

export const AnthropicAction = {
    component: AnthropicActionComponent
};
