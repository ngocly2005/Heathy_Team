# AppHealth: Android và React Admin

Bản UI chính để giáo viên push vào `ngocly2005/Heathy_Team`, học sinh pull về.
Backend tương ứng là repo riêng `ngocly2005/App_Heathy_BE`; cần source tích hợp
và migration `20261105_0011`. Xem [cấu hình](docs/SETUP.md) trước khi chạy.

Học sinh xem [hướng dẫn vào UI theo từng task](docs/HUONG_DAN_VAO_UI_THEO_TASK.md),
gồm cách mở/đăng nhập React Admin (task 5/11/16) và chat AI (task 12).

## Bản chạy chính

`MainActivity` mở `feature/integrated/ConnectedApp.kt`. Các màn đã nối API dùng
ViewModel và ngày chung, không ghi nhật ký sức khỏe vào SharedPreferences.
Giữ theme, thanh điều hướng, tiến độ calo, thẻ nước và lịch tuần từ UI hiện có.
Admin Android dùng `feature/adminconnected`, Google/session và các API Admin BE
giống React Admin web. Tài khoản `admin` vào cổng quản trị khi đăng nhập; từ giao
diện sức khỏe có thể mở lại tại Tài khoản → Quản trị viên → Mở quản trị.
Quản trị có lối về ứng dụng và đăng xuất; quyền được kiểm tra lại từ BE trước khi
tải/ghi dữ liệu, không xác định bằng email cố định. Các màn cục bộ cũ được giữ để
tham khảo; tài khoản mẫu và màn Admin dữ liệu local không phải luồng mới. Dữ liệu local cũ
không bị xóa và không tự tải lên tài khoản Google.

| Sprint/task | Source tính năng tích hợp |
|---|---|
| S1-T01 | Hồ sơ, preview BMI/BMR/TDEE, đọc/lưu BE |
| S1-T02 | Tổng ngày, calo đã ăn/vận động/còn lại |
| S1-T03 | Camera preview, picker, upload, sửa kết quả và xác nhận |
| S1-T04 | Search catalog và định lượng món theo gram |
| S1-T05 | React Admin Google login, layout, users/status |
| S2-T01 | Progress dinh dưỡng, tỷ lệ 4/4/9, mục tiêu |
| S2-T02 | Catalog vận động, thời lượng, ước tính/lưu MET |
| S2-T03 | Nước theo ngày, cộng/trừ, mục tiêu và ly 200/500 ml |
| S2-T04 | Saved meals CRUD, lưu từ nhật ký, quick-add |
| S2-T05 | Cân nặng có ngày, chỉnh/xóa, biểu đồ xu hướng |
| S2-T06 | React Admin CRUD thực phẩm/bài tập |
| S3-T01 | Chat, bữa ăn đầy đủ trong từng tin AI, nút hỏi tiếp riêng và lịch sử riêng user |
| S3-T02 | Diary tổng hợp và sửa/xóa món/vận động/nước/ghi chú |
| S3-T03 | Lịch tháng/tuần, marker BE, chọn ngày quá khứ |
| S3-T04 | React Admin charts, bộ lọc, lịch sử AI |

Yêu cầu có 15 task, bỏ số 13; S3-T02/T03/T04 tương ứng số gốc 14/15/16.
Có source không có nghĩa OAuth, AI hoặc migration thật đã được kiểm chứng;
xem [kết quả kiểm tra](docs/VALIDATION.md).

## Android

Mở root repo bằng Android Studio, dùng Gradle JDK 17 hoặc 21. Google ID SDK giữ
phiên bản tương thích Kotlin 2.2.21, không bỏ qua kiểm tra metadata.

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Google → hồ sơ → Trang chủ. Dấu `+` mở tìm món/camera. Macro, nước, vận động,
cân nặng, chat và lịch có ở Trang chủ/Tài khoản. Thực đơn là các bữa đã lưu,
thêm nhanh vào ngày/bữa/giờ đang chọn.

## React Admin

Dùng Node 22.12+ hoặc 24. Trong `admin-web`:

```powershell
npm ci
if (-not (Test-Path .env.local)) { Copy-Item .env.example .env.local }
npm run dev
```

Điền Client ID và URL BE trước khi chạy. Mở `http://127.0.0.1:5173`; origin này
cần có trong OAuth JavaScript origins và CORS. Tài khoản phải được giáo viên
cấp role tại BE. Google login không tự cấp quyền quản trị.

```powershell
npm run test
npm run build
```

Dependency URI decoder đã được vá, có lớp tương thích export query-string và
test filter tiếng Việt/sort/chuỗi encode lỗi. Xóa catalog là soft-delete, giữ log cũ.

## Git

FE và BE giữ git riêng. Thay đổi chức năng tại `code_chinh` ở working tree để
giáo viên kiểm tra, commit và push theo quy trình của mình; không push tự động.

Bộ FE theo task nằm ngoài repo tại `trien_khai_git_fe`: source cộng dồn, patch,
checksum, bảng phụ thuộc và checkout phát hành. Đích bộ đó là
`ngocly2005/App_Heathy_Team`, chỉ dưới `UI/`. Bộ BE chia sprint giữ nguyên.
Đây là cách đóng gói theo backlog, không phải lịch sử phát triển gốc; không đổi
ngày commit thành ngày sprint trong báo cáo.
