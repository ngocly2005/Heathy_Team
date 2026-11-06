# Cấu hình giáo viên và học sinh

## Android kết nối BE

Giữ `local.properties` riêng từng máy, không commit. Thêm và điền Web Client ID thật:

```properties
HEALTH_API_BASE_URL=http://10.0.2.2:8000/
GOOGLE_WEB_CLIENT_ID=
```

`10.0.2.2` dành cho emulator. Máy Android thật dùng địa chỉ LAN của máy chạy BE,
cùng mạng và cổng cho phép kết nối. URL là origin có `/` cuối, không thêm `api/v1`.
Có thể truyền cùng giá trị bằng Gradle `-P`. HTTP chỉ bật ở debug; release dùng
`HEALTH_API_RELEASE_BASE_URL` có HTTPS.

Google Cloud cần Web OAuth Client ID và Android OAuth client cho package
`com.app_health.app`, với SHA-1 của khóa ký từng máy. Xem SHA-1 bằng
`gradlew.bat signingReport`. Không sao chép private keystore/token/key vào Git.
BE xác minh ID token bằng cùng `GOOGLE_WEB_CLIENT_ID`.

BE cần cập nhật source và giáo viên tự áp migration theo README repo BE.
App không tự upgrade DB. `/health` phải truy cập được từ thiết bị.

## React Admin

Trong `admin-web/.env.local` (không commit):

```dotenv
VITE_API_BASE_URL=http://localhost:8000
VITE_GOOGLE_WEB_CLIENT_ID=
```

Điền Client ID Web thật. Thêm origin bạn mở, ví dụ `http://127.0.0.1:5173`, vào
OAuth JavaScript origins và `CORS_ORIGINS` BE. Dev cho phép HTTP tới máy LAN;
production cần HTTPS cho BE từ xa. Không đặt Groq/JWT secret vào Vite.

Giáo viên tự cấp role bằng `python -m app.admin_setup --email "email-da-dang-nhap"`
trong repo BE, thay bằng email Google đã đăng nhập và đang hoạt động. Không có
Admin mặc định; React từ chối vào quản trị nếu chưa được cấp role.

## Khi gặp lỗi ghi dữ liệu

Chuyển màn sẽ tự tải phần dữ liệu cần thiết. Dữ liệu đã tải của cùng tài khoản/ngày
được dùng lại; lưu thành công hoặc đổi ngày làm mới dữ liệu liên quan. Lỗi đọc ở
màn cũ không đi theo khi chuyển màn, và phản hồi đến muộn được bỏ qua.
GET/HEAD lỗi kết nối trước khi nhận phản hồi được thử lại tối đa một lần. Lỗi HTTP, chứng chỉ,
thao tác ghi và làm mới token không được tự gửi lại theo cơ chế này. Bản debug ghi
tag `HealthRead` với phương thức, đường dẫn và loại lỗi; không ghi token, body hay query.

Client khóa thao tác khi đang gửi. Mất kết nối sau khi gửi có thể là server đã
lưu: bấm Tải lại, xem nhật ký trước khi thao tác tiếp. Không tự retry POST.
Ngày ở Home/Diary/lịch dùng chung cho món, ảnh, nước, vận động và saved meals.
Cân nặng có ngày đo riêng. Đổi tài khoản xóa dữ liệu phiên trong bộ nhớ.

Giới hạn: không tự đồng bộ dữ liệu local cũ, không có hàng đợi ghi offline hoặc
websocket; chart cập nhật sau xác nhận BE/tải lại. Mục tiêu cho ngày cũ dùng cấu
hình hiện hành. Google thật, Groq thật và migration PostgreSQL cần cấu hình giáo viên.
