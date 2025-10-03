import {registry} from '@jahia/ui-extender';
import register from './Anthropic.register';

export default function () {
    registry.add('callback', 'anthropic-tags', {
        targets: ['jahiaApp-init:50'],
        callback: register
    });
}
