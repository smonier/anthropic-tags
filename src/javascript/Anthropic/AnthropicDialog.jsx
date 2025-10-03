import React, {useState} from 'react';
import {Dialog, DialogActions, DialogContent, DialogTitle} from '@material-ui/core';
import {Button, Dropdown, Typography, Warning} from '@jahia/moonstone';
import PropTypes from 'prop-types';
import {useTranslation} from 'react-i18next';
import styles from './AnthropicDialog.scss';
import {LoaderOverlay} from '../DesignSystem/LoaderOverlay';

export const AnthropicDialog = ({
    availableLanguages,
    isOpen,
    onCloseDialog,
    path,
    langLocale,
    formik
}) => {
    const {t} = useTranslation('anthropic-tags');
    const handleCancel = () => {
        onCloseDialog();
    };

    const defaultOption = {
        label: t('anthropic-tags:label.dialog.defaultValue'),
        value: 'void'
    };

    const [currentOption, setCurrentOption] = useState(defaultOption);

    /*    Const mergeArrayValues = (a = [], b = []) => {
        return [...new Set([...a, ...b])];
    }; */

    const handleOnChange = (e, item) => {
        setCurrentOption(item);
        return true;
    };

    const [loadingQuery, setLoadingQuery] = useState(false);

    const handleClick = async () => {
        setLoadingQuery(true);
        try {
            const formData = new FormData();
            formData.append('tagLanguage', currentOption.value);

            const response = await fetch(`${contextJsParameters.contextPath}/cms/editframe/default/${langLocale}${path}.anthropicAction.do`, {
                method: 'POST',
                headers: {Accept: 'application/json'},
                body: formData
            });

            if (!response.ok) {
                const errorMessage = `HTTP error! status: ${response.status}`;
                const errorBody = await response.text();
                console.error(errorMessage, errorBody);
                throw new Error(errorMessage);
            }

            let results;
            try {
                results = await response.json();
            } catch (error) {
                console.error('Error parsing JSON:', error);
                throw new Error('Failed to parse JSON response');
            }

            if (results.resultCode === 200) {
                console.log('Success:', results);

                if (Array.isArray(results.tags)) {
                    // Update the formik field with cleaned tags array
                    await formik.setFieldValue('jmix:tagged', true);
                    await formik.setFieldValue('jmix:tagged_j:tagList', results.tags);
                    console.log('Cleaned tags: {}', results.tags);
                }
            } else {
                console.error(`Error: ${results.resultCode}`);
            }
        } catch (error) {
            console.error('Error Accessing Anthropic:', error);

            let errorMessage = 'An error occurred while invoking Anthropic. Please check the console for more details.';
            if (error instanceof Error) {
                errorMessage = error.message;
            }

            console.error(errorMessage);
        } finally {
            setLoadingQuery(false);
        }

        onCloseDialog();
    };

    let isApplyDisabled = defaultOption.value === currentOption.value;

    return (
        <Dialog fullWidth
                aria-labelledby="alert-dialog-slide-title"
                open={isOpen}
                maxWidth="sm"
                classes={{paper: styles.dialog_overflowYVisible}}
                onClose={onCloseDialog}
        >
            <DialogTitle id="dialog-language-title" className={styles.dialogTitleContainer}>
                <img
                    src="https://upload.wikimedia.org/wikipedia/commons/b/b0/Claude_AI_symbol.svg"
                    alt="Anthropic Logo"
                    className={styles.dialogLogo}
                    width={40}
                />
                <Typography isUpperCase variant="heading" weight="bold" className={styles.dialogTitle}>
                    {t('anthropic-tags:label.dialog.dialogTitle')}
                </Typography>
                <div className={styles.dialogTitleTextContainer}>
                    <Typography variant="subheading" className={styles.dialogSubTitle}>
                        {t('anthropic-tags:label.dialog.dialogSubTitle')}
                    </Typography>
                </div>
            </DialogTitle>
            <DialogContent className={styles.dialogContent} classes={{root: styles.dialogContent_overflowYVisible}}>
                <div className={styles.loaderOverlayWrapper}>
                    <LoaderOverlay status={loadingQuery}/>
                </div>
                <Typography className={styles.copyFromLabel}>
                    {t('anthropic-tags:label.dialog.listLabel')}
                </Typography>
                <Dropdown
                    className={styles.language}
                    label={currentOption.label}
                    value={currentOption.value}
                    size="medium"
                    isDisabled={false}
                    maxWidth="120px"
                    data={[defaultOption].concat(availableLanguages.map(element => {
                        return {
                            value: element.language,
                            label: element.displayName
                        };
                    }))}
                    onChange={handleOnChange}
                />
            </DialogContent>
            <DialogActions>
                <Typography className={styles.warningText}>
                    <Warning
                        className={styles.warningIcon}/> {t('anthropic-tags:label.dialog.bottomText')}
                </Typography>
                <Button
                    size="big"
                    color="default"
                    label={t('anthropic-tags:label.dialog.btnCancel')}
                    onClick={handleCancel}
                />
                <Button
                    size="big"
                    color="accent"
                    label={t('anthropic-tags:label.dialog.btnApply')}
                    disabled={isApplyDisabled}
                    onClick={handleClick}
                />
            </DialogActions>
        </Dialog>
    );
};

AnthropicDialog.propTypes = {
    availableLanguages: PropTypes.array,
    isOpen: PropTypes.bool,
    onCloseDialog: PropTypes.func,
    path: PropTypes.string,
    langLocale: PropTypes.string,
    formik: PropTypes.object
};
