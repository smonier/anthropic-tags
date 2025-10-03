import React from 'react';
import PropTypes from 'prop-types';
import {Loader} from '@jahia/moonstone';

export const LoaderOverlay = ({status}) => (
    <div
        className="flexFluid flexCol_center alignCenter"
        style={{
            backgroundColor: 'var(--color-light)',
            display: status ? 'block' : 'none'
        }}
    >
        <Loader size="big"/>
    </div>
);

LoaderOverlay.propTypes = {
    status: PropTypes.bool.isRequired
};
