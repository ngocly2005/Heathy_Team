import { defaultTheme } from 'react-admin';
import { createTheme } from '@mui/material/styles';

const fontFamily = 'system-ui, "Segoe UI", Arial, sans-serif';

export const fitChatTheme = createTheme(defaultTheme, {
    palette: {
        mode: 'light', primary: { main: '#009e73', dark: '#007b58', light: '#dbf7ec', contrastText: '#fff' },
        secondary: { main: '#7262bc' }, background: { default: '#f5f6fb', paper: '#ffffff' },
        text: { primary: '#172336', secondary: '#697386' }, divider: '#edf0f4',
        success: { main: '#078b66' }, error: { main: '#c74c61' },
    },
    sidebar: { width: 260, closedWidth: 72 },
    shape: { borderRadius: 14 },
    typography: {
        fontFamily,
        h1: { fontFamily }, h2: { fontFamily }, h3: { fontFamily },
        h4: { fontFamily, fontSize: '2.15rem', fontWeight: 800 }, h5: { fontFamily, fontWeight: 750 }, h6: { fontFamily, fontWeight: 700 },
        subtitle1: { fontFamily }, subtitle2: { fontFamily }, body1: { fontFamily }, caption: { fontFamily }, overline: { fontFamily },
        button: { fontFamily, textTransform: 'none', fontWeight: 650 }, body2: { fontFamily, lineHeight: 1.65 },
    },
    components: {
        RaTextInput: { defaultProps: { variant: 'outlined' } },
        RaSelectInput: { defaultProps: { variant: 'outlined' } },
        RaNumberInput: { defaultProps: { variant: 'outlined' } },
        RaDateInput: { defaultProps: { variant: 'outlined' } },
        RaNullableBooleanInput: { defaultProps: { variant: 'outlined' } },
        RaList: { styleOverrides: { root: { minWidth: 0, width: '100%' } } },
        RaListToolbar: { styleOverrides: { root: {
            minWidth: 0, gap: 16, alignItems: 'center', paddingBottom: 18, flexWrap: 'wrap', background: 'transparent',
            '@media(max-width:600px)': { flexDirection: 'column', alignItems: 'stretch', flexWrap: 'nowrap' },
        } } },
        RaTopToolbar: { styleOverrides: { root: { padding: 0, minHeight: 0, gap: 8, flexWrap: 'wrap',
            '@media(max-width:600px)': { flex: '0 1 auto', width: '100%' },
        } } },
        RaFilterForm: { styleOverrides: { root: {
            flex: '1 1 auto', minWidth: 0, minHeight: 0, gap: 12, padding: 0,
            '& .RaFilterForm-filterFormInput': { minWidth: 0, flex: '0 1 220px' },
            '& .RaFilterForm-filterFormInput:first-of-type': { flex: '1 1 260px' },
            '& .MuiFormControl-root': { width: '100%', margin: 0 },
            '& .MuiOutlinedInput-root': { borderRadius: '30px' },
            '@media(max-width:600px)': { '& .RaFilterForm-filterFormInput': { flex: '1 1 100%' } },
        } } },
        MuiCssBaseline: { styleOverrides: {
            body: { margin: 0 }, '*, *::before, *::after': { boxSizing: 'border-box' },
            'a': { textDecoration: 'none' }, ':focus-visible': { outline: '3px solid #6acdaf', outlineOffset: '3px' },
        } },
        MuiTextField: { defaultProps: { variant: 'outlined' } },
        MuiButton: { defaultProps: { disableElevation: true }, styleOverrides: { root: { borderRadius: 30, minHeight: 40, paddingInline: 20 }, containedPrimary: { background: 'linear-gradient(110deg, #00ae7b, #00956c)' } } },
        MuiCard: { styleOverrides: { root: { borderRadius: 24, border: '1px solid #eff1f5', boxShadow: '0 4px 18px #27374d06', overflow: 'hidden' } } },
        MuiPaper: { styleOverrides: { rounded: { borderRadius: 20 } } },
        MuiOutlinedInput: { styleOverrides: { root: { borderRadius: 15, background: '#fff' }, notchedOutline: { borderColor: '#e0e4eb' } } },
        MuiInputLabel: { styleOverrides: { root: { color: '#697386' } } },
        MuiTableCell: { styleOverrides: {
            root: { borderColor: '#eff1f4', padding: '19px 20px', fontSize: 14 },
            head: { backgroundColor: '#fafbfc', color: '#293142', fontWeight: 700, whiteSpace: 'nowrap', fontSize: 13 },
        } },
        MuiTableRow: { styleOverrides: { root: { '&.MuiTableRow-hover:hover': { backgroundColor: '#f7fcf9' } } } },
        MuiToolbar: { styleOverrides: { root: { gap: 8 } } },
        MuiAlert: { styleOverrides: { root: { borderRadius: 15 } } },
        MuiDialog: { styleOverrides: { paper: { borderRadius: 24 } } },
    },
});

export const resourceStyles = {
    '& .RaList-main': { minWidth: 0 },
    '& .RaList-content': { minWidth: 0, maxWidth: '100%', overflow: 'hidden', borderRadius: '24px', boxShadow: '0 4px 18px #27374d06', border: '1px solid #eff1f5' },
    '& .RaList-actions': { flexWrap: 'wrap', gap: 1, px: 0, pb: 2, alignItems: 'center' },
    '& .RaFilterForm-root': { flex: 1, gap: 1, minWidth: 0 },
    '& .RaFilterForm-root .MuiOutlinedInput-root': { borderRadius: '30px' },
    '& .RaDatagrid-tableWrapper': { overflowX: 'auto' },
    '& .RaDatagrid-root': { minWidth: 0, maxWidth: '100%' },
    '& .RaDatagrid-table': { minWidth: 720 },
    '& .column-email': { minWidth: 225 },
    '& .RaDatagrid-headerCell': { bgcolor: '#fafbfc' },
    '& .RaEdit-card, & .RaCreate-card, & .RaShow-card': { borderRadius: '24px', p: { xs: 1, sm: 2 }, border: '1px solid #eff1f5', boxShadow: '0 4px 18px #27374d06' },
    '& .RaSimpleForm-form': { maxWidth: 1000, width: '100%' },
    '& .RaToolbar-toolbar': { borderRadius: '16px', bgcolor: '#f6faf8', flexWrap: 'wrap', gap: 2, py: 2 },
};
