import i18next from 'i18next';
import {registry} from '@jahia/ui-extender';
import {AnthropicAction} from './Anthropic/AnthropicAction';
import React from 'react';
import {Tag} from '@jahia/moonstone';

export default async function () {
    await i18next.loadNamespaces('anthropic-tags');

    registry.add('action', 'anthropic-tags', AnthropicAction, {
        targets: ['content-editor/header/3dots:99'],
        buttonIcon: <Tag/>,
        buttonLabel: 'anthropic-tags:label.title'
    });

    console.debug('%c anthropic-tags is activated', 'color: #3c8cba');
}
