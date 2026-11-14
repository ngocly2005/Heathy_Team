# Bảng hướng dẫn vào UI theo 3 sprint

Cập nhật **14/11/2026**. Có **16 dòng tương ứng 32 task FE/BE**; hai task cùng dòng dùng chung luồng UI.

| Task | Sprint | Tính năng | Vào UI hiện tại |
|---|---|---|---|
| S1-BL02-FE / S1-BL03-BE | S1 | Đăng ký / đăng nhập | Android → màn đầu → **Đăng ký ngay / Đăng nhập / Tiếp tục với Google** |
| S1-BL06-FE / S1-BL07-BE | S1 | Hồ sơ, BMI/TDEE | Android → **Tài khoản → Cập nhật chỉ số**; hoặc **Hồ sơ thể chất → Chỉnh sửa thông tin** |
| S1-BL10-FE / S1-BL11-BE | S1 | Tổng ngày và tiến độ calo | Android → **Trang chủ → Mục tiêu calo / Calo còn lại** |
| S1-BL14-FE / S1-BL15-BE | S1 | Camera và upload ảnh | Android → **+ → Chụp ảnh món ăn**; hoặc **Tài khoản → Chụp món ăn** |
| S1-BL18-FE / S1-BL19-BE | S1 | Tìm món và chỉnh khẩu phần | Android → **+ → Chọn món ăn thủ công**; hoặc **Tài khoản → Tìm món ăn** |
| S1-BL22-FE / S1-BL23-BE | S1 | Admin login/layout/người dùng | Admin web → đăng nhập **email hoặc Google có quyền Admin → Người dùng** |
| S2-BL26-FE / S2-BL27-BE | S2 | Dinh dưỡng, mục tiêu và macro | Android → **Trang chủ → Chi tiết dinh dưỡng**; hoặc **Tài khoản → Dinh dưỡng** |
| S2-BL30-FE / S2-BL31-BE | S2 | Loại vận động và thời gian | Android → **Tài khoản → Vận động**; hoặc **Trang chủ → Ghi nhận vận động** |
| S2-BL34-FE / S2-BL35-BE | S2 | Nước uống và tiến độ ly | Android → **Tài khoản → Uống nước**; hoặc **Nhật ký → Cài đặt & ghi thêm nước** |
| S2-BL38-FE / S2-BL39-BE | S2 | Bữa ăn đã lưu và thêm nhanh | Android → tab **Thực đơn → Lưu bữa ăn / Thêm nhanh vào nhật ký** |
| S2-BL42-FE / S2-BL43-BE | S2 | Cân nặng và biểu đồ tháng | Android → **Tài khoản → Cân nặng**; hoặc hồ sơ → **Xem biểu đồ cân nặng** |
| S2-BL46-FE / S2-BL47-BE | S2 | Admin quản lý thực phẩm/bài tập | Admin web → **Thực phẩm / Bài tập → Thêm / Sửa / Ngừng sử dụng** |
| S3-BL50-FE / S3-BL51-BE | S3 | Chat AI và gợi ý bữa ăn | Android → **Tài khoản → cuộn xuống → Tư vấn bữa ăn** |
| S3-BL54-FE / S3-BL55-BE | S3 | Nhật ký tổng hợp và chỉnh/xóa | Android → tab **Nhật ký → chọn ngày → Sửa / Xóa** tại mục cần chỉnh |
| S3-BL58-FE / S3-BL59-BE | S3 | Lịch và ngày quá khứ | Android → **Tài khoản → Lịch sử ngày**; hoặc **Nhật ký → tiêu đề ngày / biểu tượng lịch** |
| S3-BL62-FE / S3-BL63-BE | S3 | Admin biểu đồ và lịch sử AI | Admin web → **Dashboard / Lịch sử AI → Chi tiết** |

**Admin web** mở trên trình duyệt: tại `admin-web`, chạy `npm run dev` và mở địa chỉ Vite (mặc định `http://127.0.0.1:5173`). Cần tài khoản được BE cấp role `admin`.

**Admin Android**: đăng nhập tài khoản Admin trên app; từ phần sức khỏe vào **Tài khoản → Quản trị viên → Mở quản trị**. Các task ghi React Admin cần trình diễn trên web.

**Lưu ý:** chọn đúng tài khoản/ngày; BE phải đang chạy. Task BE được kiểm tra qua dữ liệu UI, không có màn BE riêng.

<details>
<summary>Hướng dẫn thực hành chi tiết và kết quả cần kiểm tra</summary>

# Hướng dẫn sử dụng UI theo 32 task của 3 sprint

Cập nhật: **14/11/2026**. Dùng cho bản chính trong repo **Heathy_Team** (Android
và Admin web), kết nối repo **App_Heathy_BE**.

Lịch có **16 task FE + 16 task BE**. Mỗi dòng dưới ghép hai task cùng tính năng.
Task BE được kiểm tra thông qua dữ liệu/thao tác trên UI tương ứng; **không có
một màn BE riêng** để học sinh mở. Mã BL là mã của lịch sprint mới, khác cách
đánh số 1–16 trong danh sách FE ban đầu.

## 1. Chuẩn bị trước khi thực hành

1. Pull nhánh `main` của **từng repo riêng** FE và BE theo hướng dẫn giáo viên.
2. BE phải chạy, kết nối database đã được giáo viên chuẩn bị; kiểm tra `/health`.
3. Mở repo FE bằng Android Studio → chạy module `app` trên emulator/điện thoại.
   Địa chỉ BE và Google Client ID được cài trong `local.properties` theo [SETUP.md](SETUP.md).
4. Có thể đăng ký bằng email/mật khẩu để dùng app. Google cần cấu hình OAuth
   riêng; không bắt buộc dùng Google để thực hành các chức năng sức khỏe.
5. Vào **Tài khoản → Cập nhật chỉ số**, lưu hồ sơ trước khi thử calo, vận động,
   dinh dưỡng và tư vấn AI. Chuẩn bị ít nhất một món/bài tập trong catalog BE.

| Nơi chạy | Cách mở | Tài khoản |
|---|---|---|
| Android người dùng | Android Studio → Run `app` | Email/mật khẩu hoặc Google |
| Admin Android | Cùng app, đăng nhập tài khoản có role `admin` | Quyền cấp từ BE |
| React Admin web | Terminal tại `admin-web`: lần đầu `npm ci`, sau đó `npm run dev`; mở địa chỉ Vite hiển thị, mặc định `http://127.0.0.1:5173` | Email/mật khẩu hoặc Google của Admin |

Admin web cần `.env.local` theo `.env.example`, địa chỉ BE/CORS đúng; Google
cần đăng ký đúng origin của tab. Học sinh không tự sửa role qua form đăng ký.
Giáo viên cấp quyền theo phần Admin trong [hướng dẫn chi tiết](HUONG_DAN_VAO_UI_THEO_TASK.md).

**Điểm cần nhớ khi thao tác:**

- Thanh dưới Android: **Trang chủ / Nhật ký / + / Thực đơn / Tài khoản**.
- Nút **+** mở lựa chọn thêm món thủ công hoặc chụp ảnh.
- Trong **Tài khoản**, cuộn xuống để thấy Dinh dưỡng, Vận động, Uống nước,
  Cân nặng, Tư vấn bữa ăn và Lịch sử ngày.
- Dữ liệu sức khỏe phụ thuộc **tài khoản và ngày đang chọn**. Chọn cùng ngày
  khi đối chiếu hai màn; không lấy số liệu minh họa trong ảnh thiết kế làm chuẩn.
- Nếu Admin Android tự mở sau đăng nhập, chọn **Về ứng dụng** để thực hành
  phần người dùng. Mở lại qua **Tài khoản → Quản trị viên → Mở quản trị**.

## 2. Sprint 1 — 12 task / 6 nhóm tính năng

### 2.1. Đăng ký, đăng nhập — S1-BL02-FE / S1-BL03-BE

**Đường vào:** Màn đầu Android → **Đăng ký ngay** hoặc form **Đăng nhập**.

1. Nhập họ tên, email có dạng `ten@gmail.com`, mật khẩu mới 8–128 ký tự và
   xác nhận giống hệt. Mật khẩu mới không chỉ gồm khoảng trắng hoặc chứa ký tự
   điều khiển. App hiện lý do tại ô nhập nếu chưa hợp lệ; đủ điều kiện mới bật nút.
2. Đăng ký email mới → mở Tài khoản để kiểm tra danh tính → Đăng xuất.
3. Đăng nhập lại đúng email/mật khẩu. Thử sai mật khẩu hoặc đăng ký trùng email
   để kiểm tra thông báo; không dùng tài khoản thật của người khác.
4. Có thể thử **Tiếp tục với Google** nếu đã cấu hình. Trong **Tài khoản → Bảo
   mật tài khoản**, thêm/đổi mật khẩu hoặc liên kết Google đúng email hiện tại.

**Đạt khi:** đúng thông tin đăng nhập được; sai/trùng bị chặn; đăng nhập lại giữ
đúng hồ sơ và dữ liệu. Google và email đã liên kết dùng chung tài khoản/quyền.
Không có luồng quên mật khẩu qua email trong bản hiện tại.

### 2.2. Hồ sơ và BMI/TDEE — S1-BL06-FE / S1-BL07-BE

**Đường vào:** **Tài khoản → Cập nhật chỉ số**; hoặc **Hồ sơ thể chất → Chỉnh sửa thông tin**.

Nhập ngày sinh, giới tính, chiều cao **cm**, cân nặng **kg**, mức vận động và mục
tiêu. Xem phần tính trước BMI/BMR/TDEE → **Lưu thay đổi**. Quay lại hồ sơ, rồi
mở lại form để kiểm tra dữ liệu.

**Đạt khi:** chỉ số được tính từ dữ liệu vừa nhập, hồ sơ lưu lại được và hiển thị
đúng sau khi rời màn. Không dùng số 0/ô chưa có dữ liệu để kết luận sức khỏe.

### 2.3. Tiến độ calo — S1-BL10-FE / S1-BL11-BE

**Đường vào:** **Trang chủ** → chọn ngày trên lịch tuần.

Xem vòng calo còn lại, lượng đã ăn/mục tiêu, thẻ Đã nạp và Tập luyện. Thêm một
món theo mục 2.5, quay lại cùng ngày để đối chiếu. Ghi vận động theo mục 3.2
để kiểm tra tổng vận động.

**Đạt khi:** tổng thay đổi theo nhật ký thật. Tiến độ đã ăn = đã ăn/mục tiêu;
calo còn lại = mục tiêu − đã ăn + vận động. Không nhầm hai đại lượng này.

### 2.4. Camera và upload ảnh — S1-BL14-FE / S1-BL15-BE

**Đường vào:** **+ → Chụp ảnh món ăn**, hoặc **Tài khoản → Chụp món ăn**.

Cấp quyền camera → chụp hoặc chọn ảnh từ thư viện → xem trước → **Phân tích
ảnh**. Kiểm tra/sửa kết quả tên món, gram, calo và đa lượng; chọn bữa/giờ ăn →
**Xác nhận và lưu nhật ký**. Mở Nhật ký đúng ngày để kiểm tra.

**Đạt khi:** có preview, upload và kết quả hoặc lỗi rõ ràng; chỉ ghi món sau khi
xác nhận. Emulator không có camera có thể dùng ảnh thư viện. AI cần BE/provider
được cấu hình; chỉ chọn ảnh chưa chứng minh đã ghi nhật ký.

### 2.5. Tìm món và khẩu phần — S1-BL18-FE / S1-BL19-BE

**Đường vào:** **+ → Chọn món ăn thủ công**, hoặc **Tài khoản → Tìm món ăn**.

Tìm tên → chọn món/dấu cộng cạnh món → nhập gram → xem calo/đa lượng tính lại →
chọn bữa, giờ `HH:mm` → **Thêm vào nhật ký**. Thử tìm từ khóa không có kết quả.

**Đạt khi:** kết quả lấy từ catalog BE, lượng dinh dưỡng đổi theo gram; món xuất
hiện ở đúng ngày/bữa/giờ trong Nhật ký, tổng Trang chủ cập nhật.

### 2.6. React Admin, đăng nhập và người dùng — S1-BL22-FE / S1-BL23-BE

**Đường vào bắt buộc để trình diễn task React:** trình duyệt → Admin web →
đăng nhập tài khoản Admin → sidebar **Người dùng**.

Tìm tên/email, lọc trạng thái, đổi trang, mở **Sửa** → thay trạng thái → **Lưu
thay đổi**. Có thể xuất tối đa 100 dòng theo bộ lọc. Chuẩn bị một user thử nghiệm
để khóa/mở; BE không cho khóa Admin tại đây.

**Đạt khi:** user thường bị từ chối vào quản trị, Admin đọc được dữ liệu thật và
thay trạng thái user. Không có nút tạo/xóa user hoặc form tự cấp quyền Admin.
Admin Android có chức năng tương ứng, nhưng không thay thế phần trình diễn React web.

## 3. Sprint 2 — 12 task / 6 nhóm tính năng

### 3.1. Dinh dưỡng và macro — S2-BL26-FE / S2-BL27-BE

**Đường vào:** **Trang chủ → Chi tiết dinh dưỡng**, hoặc **Tài khoản → Dinh dưỡng**.

Ở Thiết lập mục tiêu, nhập calo/ngày hoặc để trống dùng mục tiêu từ hồ sơ; nhập
tỷ lệ đạm/bột/béo, tổng phải bằng **100%** → **Lưu mục tiêu**. Xem tổng hợp mục
tiêu đã lưu và phần dinh dưỡng trong ngày; thêm món rồi đối chiếu các thanh/biểu đồ.

**Đạt khi:** mục tiêu lưu lại, thanh gram và tỷ lệ năng lượng thay đổi theo dữ liệu
đã ăn; tỷ lệ mục tiêu không bị nhầm với tỷ lệ thực tế. Chất xơ chưa thuộc tính năng.

### 3.2. Vận động và thời gian — S2-BL30-FE / S2-BL31-BE

**Đường vào:** **Tài khoản → Vận động**, hoặc nút ghi vận động ở Trang chủ/Nhật ký.

Chọn ngày, tìm/chọn loại vận động trong catalog, nhập phút hoạt động → xem ước
tính → **Lưu vận động**. Kiểm tra Trang chủ và Nhật ký cùng ngày.

**Đạt khi:** có bài tập/thời lượng, calo được tính theo hồ sơ và MET; lưu rồi đọc
lại được. Nếu catalog trống, Admin cần thêm bài tập ở mục 3.6. Chưa nhập quãng đường km.

### 3.3. Theo dõi nước — S2-BL34-FE / S2-BL35-BE

**Đường vào:** **Tài khoản → Uống nước**; hoặc nút cài đặt/ghi thêm nước trong Nhật ký.

Chọn ngày → **+ / −** theo dung tích ly. Nhập mục tiêu tùy chỉnh hoặc để trống tự
tính, chọn dung tích ly → **Lưu cài đặt nước**. Xem tổng ml, số ly và tiến độ.

**Đạt khi:** tổng cùng ngày khớp Trang chủ/Nhật ký, không âm, cài đặt còn sau khi
mở lại. Nút trừ không phải thao tác xóa mọi log nước trong ngày.

### 3.4. Thực đơn lưu và thêm nhanh — S2-BL38-FE / S2-BL39-BE

**Đường vào:** tab **Thực đơn**.

Ghi ít nhất một món trước → chọn ngày có món → tại **Lưu bữa từ nhật ký ngày đã
chọn**, nhập tên, chọn món → **Lưu bữa ăn**. Thử sửa tên/khẩu phần hoặc xóa bữa
thử nghiệm. Chọn ngày đích, bữa và giờ → **Thêm nhanh vào nhật ký**.

**Đạt khi:** bữa lưu còn trong danh sách khi mở lại, thêm nhanh tạo log ở đúng
ngày đích. Bữa lưu tối đa 30 món; xóa mẫu bữa khác với xóa món đã ghi trong Nhật ký.

### 3.5. Cân nặng và xu hướng — S2-BL42-FE / S2-BL43-BE

**Đường vào:** **Tài khoản → Cân nặng**, hoặc **Xem biểu đồ cân nặng** trên hồ sơ.

Tại Ghi số đo chọn ngày, nhập kg → **Lưu số đo**. Dùng mũi tên đổi tháng của biểu
đồ. Chọn một số đo quá khứ → **Sửa** → **Lưu thay đổi**; thử **Xóa** và xác nhận.

**Đạt khi:** biểu đồ/danh sách theo tháng cập nhật từ số đo thật. Cho sửa/xóa quá
khứ; chặn ngày tương lai. Ghi lại cùng ngày cập nhật số đo ngày đó. Cần giữ ít
nhất một số đo cho hồ sơ; thêm số đo khác trước khi thử xóa bản cuối.

### 3.6. Admin quản lý catalog — S2-BL46-FE / S2-BL47-BE

**Đường vào:** Admin web → sidebar **Thực phẩm** hoặc **Bài tập**.

Thực phẩm: **Thêm thực phẩm** → tên, calo/đạm/bột/béo trên **100 g** và trạng
thái → lưu. Bài tập: thêm tên, **MET** và trạng thái → lưu. Mở **Sửa**, cập nhật,
hoặc **Ngừng sử dụng** và xác nhận. Tìm/lọc để kiểm tra kết quả.

**Đạt khi:** app Android tìm thấy các mục đang dùng, dữ liệu chỉnh sửa đọc lại
được. Ngừng sử dụng giữ lịch sử nhật ký cũ. Dùng web React để trình diễn task;
Admin Android dùng chung BE cũng có quản lý catalog.

## 4. Sprint 3 — 8 task / 4 nhóm tính năng

### 4.1. Chat tư vấn bữa ăn — S3-BL50-FE / S3-BL51-BE

**Đường vào:** **Tài khoản → cuộn xuống → Tư vấn bữa ăn**.

Chọn ngày, nhập câu hỏi (ví dụ “Gợi ý bữa trưa theo mục tiêu của tôi”) → **Gửi**.
Đọc/cuộn phản hồi, mở lại màn để xem lịch sử. Bấm chip hỏi tiếp (ví dụ Chi tiết
bữa trưa): nội dung chỉ điền vào ô nhập, xem lại rồi mới bấm Gửi.

**Đạt khi:** chi tiết các bữa ăn nằm trong tin AI, cuộn cùng lịch sử; chip không
tự gửi. Lịch sử thuộc đúng tài khoản. Cần BE/provider AI hoạt động; câu trả lời
là gợi ý tham khảo, chưa tự ghi món vào nhật ký.

### 4.2. Nhật ký thống nhất — S3-BL54-FE / S3-BL55-BE

**Đường vào:** tab **Nhật ký** → chọn ngày.

Xem món theo giờ đã lưu, vận động, nước và ghi chú/tâm trạng. Thử sửa gram món,
phút vận động hoặc log nước; thử xóa log thử nghiệm và xác nhận. Nhập ghi chú,
chọn tâm trạng → **Lưu ghi chú**; cần có nội dung để lưu.

**Đạt khi:** thao tác cập nhật đúng mục/đúng ngày, tổng calo/đa lượng/nước đổi
tương ứng. Mở lại hoặc sang Trang chủ cùng ngày để đối chiếu dữ liệu đã lưu.

### 4.3. Lịch và ngày quá khứ — S3-BL58-FE / S3-BL59-BE

**Đường vào:** **Tài khoản → Lịch sử ngày**; cũng có thể mở lịch từ tiêu đề ngày Nhật ký.

Đổi tháng bằng mũi tên → xem dấu ngày có dữ liệu/chú giải → chọn hôm nay hoặc
ngày quá khứ → xem Nhật ký ngày đó. Dùng **Về hôm nay** trên màn lịch khi cần.

**Đạt khi:** ngày chọn và dữ liệu cùng ngày khớp nhau, dấu ngày phản ánh log thực;
không chọn ngày tương lai. Trước khi thêm/sửa log cần kiểm tra ngày trên màn.

### 4.4. Admin thống kê và lịch sử AI — S3-BL62-FE / S3-BL63-BE

**Đường vào:** Admin web → **Dashboard** và **Lịch sử AI**.

Dashboard: chọn Từ/Đến ngày → **Xem báo cáo**, hoặc chọn **7/30/90 ngày**. Xem
5 thẻ tổng, biểu đồ hoạt động và tài khoản mới/nước; mở bảng số liệu theo ngày.
Lịch sử AI: lọc ngày, thêm lọc loại/ID user nếu cần → mở **Chi tiết** để xem yêu
cầu, trạng thái, mã lỗi và phản hồi JSON đầy đủ. Có thể xuất tối đa 100 dòng.

**Đạt khi:** dữ liệu theo kỳ thật; request AI vừa thực hiện được tìm thấy ở tài
khoản/ngày phù hợp. Quyền quản trị vẫn được BE kiểm tra. Không có số tăng trưởng,
thông báo hoặc model/token AI giả từ ảnh thiết kế. Admin Android có trang tương ứng.

## 5. Trình tự demo gợi ý và bằng chứng cần lưu

Đăng ký → hồ sơ → mục tiêu dinh dưỡng → tìm món/chụp ảnh → vận động → nước →
Trang chủ → Nhật ký sửa/xóa → lưu bữa/thêm nhanh → cân nặng → lịch quá khứ →
chat AI. Sau đó dùng Admin web trình diễn người dùng, catalog và thống kê/AI log.

Mỗi nhóm task lưu: mã FE/BE, tài khoản thử, ngày dữ liệu, ảnh trước thao tác,
ảnh sau thao tác và kết quả khi mở lại. Dùng dữ liệu thử cho sửa/xóa/khóa tài
khoản. Không đưa mật khẩu, token, khóa API vào ảnh báo cáo.

Nếu nút chưa bật, đọc lỗi ngay tại ô nhập và kiểm tra hồ sơ/ngày. Nếu màn trống,
kiểm tra bộ lọc, đúng user/ngày và catalog. Nếu báo mất kết nối, kiểm tra BE và
địa chỉ mạng; không bấm lặp thao tác ghi khi chưa rõ lần trước đã lưu hay chưa.

Hướng dẫn này mô tả đường vào theo source, không thay thế kiểm thử trên thiết bị
và BE thật. Chi tiết cấu hình: [SETUP.md](SETUP.md); hướng dẫn quản trị/quyền:
[HUONG_DAN_VAO_UI_THEO_TASK.md](HUONG_DAN_VAO_UI_THEO_TASK.md).

</details>
