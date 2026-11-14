# Giao diện Admin web — 14/11/2026

Admin web trong `admin-web/` được thiết kế lại theo ba ảnh tham chiếu của giáo
viên: Tổng quan (hai ảnh cùng màn) và Người dùng. Các trang còn lại dùng cùng
phong cách nền mint/xám nhạt, sidebar trắng, thẻ bo góc, chữ đậm và nút xanh.
Tên hiển thị là **FitChat**; vẫn là React Admin web của dự án AppHealth, dùng
chung tài khoản, quyền và dữ liệu với Android qua BE hiện tại.

## Các trang đã áp dụng

| Trang | Cách trình bày |
|---|---|
| Đăng nhập | Logo lá, bố cục hai cột trên desktop, form bo góc và nút chính; email/mật khẩu và Google vẫn song song |
| Tổng quan | Bộ lọc ngày, 5 thẻ số liệu, biểu đồ xanh lá/xanh dương/tím và bộ chọn 7/30/90 ngày |
| Người dùng | Avatar chữ cái, email, nhãn quyền/trạng thái, tìm tên/email, lọc trạng thái, xuất dữ liệu và phân trang |
| Trạng thái người dùng | Thẻ thông tin tài khoản và form cập nhật trạng thái đang hoạt động/đã khóa |
| Thực phẩm | Bảng dinh dưỡng trên 100 gram, trạng thái sử dụng, tìm kiếm/lọc và nút thêm/sửa |
| Thêm/sửa thực phẩm | Form tên và các giá trị dinh dưỡng, lưu hoặc xác nhận ngừng sử dụng |
| Bài tập | Danh sách có biểu tượng, MET, trạng thái và các thao tác quản lý |
| Thêm/sửa bài tập | Form tên, MET và trạng thái; giữ giới hạn nhập và xác nhận ngừng sử dụng |
| Lịch sử AI | Bảng/bộ lọc ngày, loại yêu cầu, ID người dùng và nhãn thành công/lỗi |
| Chi tiết AI | Các thông tin yêu cầu và toàn bộ phản hồi JSON, tự xuống dòng để đọc được nội dung dài |

Thanh bên thu gọn được; trên màn nhỏ dùng menu đóng/mở và cuộn ngang riêng
trong bảng. Trạng thái và vai trò luôn có chữ, không chỉ thể hiện bằng màu.
Nội dung trống, đang tải và lỗi được giữ rõ ràng. Biểu đồ có bảng số liệu mở rộng
để đọc bằng bàn phím hoặc trình đọc màn hình.

Bộ chọn 7/30/90 ngày dùng cùng API báo cáo đã có: tính đủ số ngày bao gồm ngày
kết thúc của báo cáo đang áp dụng. Bộ lọc ngày thủ công và nút Xem báo cáo vẫn
dùng được. Biểu đồ tài khoản mới/nước được giữ, dù không có trong ảnh mẫu.

## Các chi tiết mẫu không tạo thêm chức năng

Nút xuất CSV giới hạn rõ **tối đa 100 dòng đang lọc** để khớp giới hạn mỗi lần
đọc của API hiện tại. Tránh dùng mặc định 1.000 dòng của React Admin gây lỗi
validation; đợt này không tạo API xuất toàn bộ dữ liệu.

- Không chép số liệu tài khoản hoặc phần trăm tăng trưởng trong ảnh. Các KPI
  lấy từ BE; thay dòng so sánh kỳ trước bằng chú thích phạm vi số liệu thực tế.
- Không dựng chuông/số thông báo hoặc trang Cài đặt vì chưa có chức năng tương
  ứng. Menu tài khoản và Đăng xuất vẫn hoạt động với phiên đăng nhập hiện tại.
- Không thêm nút tạo/xóa người dùng hoặc bộ lọc theo vai trò. BE hiện cho phép
  tìm tài khoản, xem quyền và cập nhật trạng thái, không có các thao tác trên.
- Không cho sửa vai trò qua form, không cố định email Admin. Quyền vẫn do BE
  xác định. Ngừng sử dụng thực phẩm/bài tập là chức năng đã có, khác với xóa user.
- Không thêm ảnh món, dấu xác thực hoặc chỉ số AI chưa có trong dữ liệu.

## Chạy và kiểm tra

Mở `admin-web/`, chuẩn bị `.env.local` theo `.env.example`, chạy `npm ci` rồi
`npm run dev`. Vào địa chỉ Vite hiển thị (mặc định `http://127.0.0.1:5173`).
Đăng nhập bằng email/mật khẩu hoặc Google của tài khoản được BE cấp quyền
Admin. Cấu hình origin Google/CORS giữ theo [SETUP.md](SETUP.md).

Các API/auth provider và cơ chế lưu/xác nhận vẫn giữ; không thêm dependency,
endpoint hoặc migration. Xem [VALIDATION.md](VALIDATION.md) cho kết quả kiểm tra
và giới hạn của dữ liệu giả trong kiểm thử giao diện.
