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

## Admin trên Android dùng cùng BE

Cùng tài khoản Google đã cấp `role=admin` có thể quản trị cả trên Android và web.
Android mở cổng quản trị khi đăng nhập tài khoản Admin; có thể chọn về ứng dụng
sức khỏe, sau đó mở lại qua **Tài khoản → Quản trị viên → Mở quản trị**.
Màn quản trị đọc lại `/auth/me` và dùng `/admin/*`; BE quyết định quyền ở từng
yêu cầu. App không dùng email/mật khẩu mẫu hoặc danh sách Admin riêng.

Nếu giáo viên đổi role trong DB khi Android đang mở, trở về màn Tài khoản và bấm
**Tải lại**, hoặc đưa app ra nền rồi mở lại để cập nhật danh tính. Sau khi thu hồi
quyền, BE từ chối các API Admin; app xóa phần dữ liệu quản trị đã tải khi bị từ chối.
Hai UI dùng cùng database qua BE, nên sửa danh mục trên một bên rồi tải lại ở bên
còn lại để xem thay đổi. Không có đồng bộ thời gian thực giữa hai UI.

## Chat: thực đơn và câu hỏi tiếp

Mỗi câu trả lời AI hiển thị đầy đủ `answer` và các chi tiết bữa ăn `suggestions`
trong lịch sử cuộn. Lịch sử cũ vẫn đọc được các chi tiết đó. Trường mới
`follow_up_questions` chỉ chứa câu hỏi ngắn để gợi ý nhập tiếp; bấm gợi ý điền vào
ô soạn, không tự gửi. Nếu bản ghi cũ chưa có trường này, app dùng các câu hỏi mẫu
như “Chi tiết bữa trưa”, “Đổi sang món chay”. Lịch sử gửi cho AI bao gồm cả các bữa
đã gợi ý, để câu hỏi tiếp có đủ ngữ cảnh. Đây là thay đổi payload JSON, không cần
migration mới; cập nhật BE và Android cùng đợt. React Admin tiếp tục xem được AI logs.

## Khi gặp lỗi ghi dữ liệu

Chuyển màn sẽ tự tải phần dữ liệu cần thiết. Dữ liệu đã tải của cùng tài khoản/ngày
được dùng lại; lưu thành công hoặc đổi ngày làm mới dữ liệu liên quan. Lỗi đọc ở
màn cũ không đi theo khi chuyển màn, và phản hồi đến muộn được bỏ qua.
GET/HEAD lỗi kết nối trước khi nhận phản hồi được thử lại tối đa một lần. GET JSON
thành công có body tối đa 1 MiB cũng được kiểm tra truyền đủ trước khi đọc dữ liệu;
body không biết độ dài chỉ kiểm tra một phần đầu có giới hạn. Không đảm bảo phục hồi
phần cuối của body lớn hoặc luồng dữ liệu. JSON sai cú pháp/khác schema được báo
là dữ liệu chưa hợp lệ, không đổi thành thông báo mất mạng hay tự gửi lại.
Lỗi HTTP, chứng chỉ,
thao tác ghi và làm mới token không được tự gửi lại theo cơ chế này. Bản debug ghi
tag `HealthRead` với phương thức, đường dẫn và loại lỗi; không ghi token, body hay query.

Client khóa thao tác khi đang gửi. Mất kết nối sau khi gửi có thể là server đã
lưu: bấm Tải lại, xem nhật ký trước khi thao tác tiếp. Không tự retry POST.
Ngày ở Home/Diary/lịch dùng chung cho món, ảnh, nước, vận động và saved meals.
Cân nặng có ngày đo riêng. Đổi tài khoản xóa dữ liệu phiên trong bộ nhớ.

## Đọc tiến độ và chỉnh số đo

Thanh calo ở Trang chủ biểu thị lượng **đã ăn / mục tiêu**. Calo còn lại lấy từ BE:
`mục tiêu − đã ăn + vận động`. Vì vậy thêm món vẫn tăng tiến độ khi lượng vận động
đang lớn hơn lượng đã ăn. Dữ liệu nhật ký được tải lại tự động sau khi lưu thành công.

Màn Cân nặng có nút đổi tháng; biểu đồ và danh sách chỉ hiện số đo trong tháng đó.
Chọn ngày bằng lịch để thêm số đo quá khứ. Bấm biểu tượng Sửa/Xóa trong danh sách
để thay đổi đúng ngày đã ghi. Có thể nhập dấu phẩy hoặc dấu chấm, tối đa hai chữ số
thập phân. Ngày tương lai không được ghi; hồ sơ cần giữ ít nhất một số đo, nên thêm
số đo khác trước khi xóa bản ghi cuối cùng. Sửa số đo cũ không thay cân nặng mới nhất.

Giới hạn: không tự đồng bộ dữ liệu local cũ, không có hàng đợi ghi offline hoặc
websocket; chart cập nhật sau xác nhận BE/tải lại. Mục tiêu cho ngày cũ dùng cấu
hình hiện hành. Google thật, Groq thật và migration PostgreSQL cần cấu hình giáo viên.
