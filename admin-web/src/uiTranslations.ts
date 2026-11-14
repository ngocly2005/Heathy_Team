import { defaultI18nProvider, type I18nProvider } from 'react-admin';

const messages: Record<string, string> = {
    'ra.auth.logout': 'Đăng xuất', 'ra.auth.user_menu': 'Tài khoản quản trị',
    'ra.action.refresh': 'Tải lại', 'ra.action.edit': 'Chỉnh sửa', 'ra.action.show': 'Chi tiết',
    'ra.action.create': 'Thêm mới', 'ra.action.save': 'Lưu', 'ra.action.cancel': 'Hủy',
    'ra.action.confirm': 'Xác nhận', 'ra.action.delete': 'Ngừng sử dụng', 'ra.action.export': 'Xuất dữ liệu',
    'ra.action.list': 'Danh sách', 'ra.action.add_filter': 'Thêm bộ lọc',
    'ra.action.remove_filter': 'Bỏ bộ lọc', 'ra.action.clear_input_value': 'Xóa nội dung nhập',
    'ra.action.sort': 'Sắp xếp', 'ra.action.open_menu': 'Mở menu', 'ra.action.close_menu': 'Đóng menu',
    'ra.action.expand': 'Mở rộng', 'ra.action.close': 'Đóng',
    'ra.navigation.no_results': 'Không có dữ liệu phù hợp.',
    'ra.navigation.no_filtered_results': 'Không tìm thấy dữ liệu theo bộ lọc hiện tại.',
    'ra.navigation.clear_filters': 'Xóa bộ lọc',
    'ra.navigation.page_rows_per_page': 'Số dòng mỗi trang:',
    'ra.navigation.page_range_info': '%{offsetBegin}–%{offsetEnd} trong %{total}',
    'ra.navigation.next': 'Trang sau', 'ra.navigation.previous': 'Trang trước',
    'ra.navigation.skip_nav': 'Đến nội dung chính', 'ra.message.loading': 'Đang tải…',
};

export const uiTranslations: I18nProvider = {
    ...defaultI18nProvider,
    translate: (key, options) => messages[key]
        ? messages[key].replace(/%\{(\w+)\}/g, (_, name: string) => String(options?.[name] ?? ''))
        : defaultI18nProvider.translate(key, options),
};
