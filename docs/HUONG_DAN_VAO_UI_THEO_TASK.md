# Hướng dẫn vào UI và thực hành theo từng task

Cập nhật: **08/11/2026**. Dành cho học sinh dùng bản chính của AppHealth.

Tài liệu dùng đúng tên nút/màn trong source hiện tại. Đây là hướng dẫn thao tác,
không phải xác nhận rằng Google, camera và AI đã được kiểm thử trên mọi máy học sinh.

**Cần tìm nhanh:**

- **Task 5, 11, 16 — Admin:** có cả Admin Android và React Admin trên trình duyệt, theo mục 3.
- **Task 12 — Chat AI:** Android → **Tài khoản** → cuộn xuống → **Tư vấn bữa ăn**.
- Danh sách gốc đánh số đến 16 nhưng có **15 task**, bỏ số 13. Tài liệu giữ nguyên số gốc.

## 1. Phân biệt app Android, trang Admin và backend

| Thành phần | Mở ở đâu? | Source/repo |
|---|---|---|
| App người dùng | Emulator hoặc điện thoại Android | Repo `Heathy_Team`, thư mục `app` |
| Admin Android | Cùng app Android, dùng tài khoản có quyền `admin` từ BE | Repo `Heathy_Team`, `feature/adminconnected` |
| React Admin | Trình duyệt trên máy tính | Repo `Heathy_Team`, thư mục `admin-web` |
| Backend (BE) | Chạy server để Android và Admin cùng kết nối | Repo riêng `App_Heathy_BE` |

Repo FE: <https://github.com/ngocly2005/Heathy_Team>.
Repo BE: <https://github.com/ngocly2005/App_Heathy_BE>.

Nếu máy đang đặt tên hai thư mục là `frontend` và `backend`, hãy đối chiếu repo
tương ứng; tên thư mục trên máy không bắt buộc giống tên trên GitHub.

**Admin Android và React Admin web cùng được giữ lại.** Cả hai dùng cùng tài khoản
Google, quyền `admin` và database thông qua BE. Sau khi cấp quyền, đăng nhập lại
Android để mở cổng quản trị; có thể bấm **Về ứng dụng** để sử dụng phần sức khỏe.
Từ phần sức khỏe, mở lại qua **Tài khoản → Quản trị viên → Mở quản trị**.

## 2. Chuẩn bị app Android và nhận biết thanh điều hướng

### 2.1. Điều kiện để dùng được tính năng

1. Pull bản FE và BE mới nhất trong **từng repo riêng**: `git pull origin main`.
2. BE đã được cấu hình database và migration theo hướng dẫn giáo viên, đang chạy
   và truy cập được `/health`. Xem [README backend](https://github.com/ngocly2005/App_Heathy_BE/blob/main/README.md).
3. Android đã có Web OAuth Client ID đúng và địa chỉ BE trong `local.properties`;
   package/SHA-1 của máy đã đăng ký Google. Xem [hướng dẫn cấu hình](SETUP.md).
4. Mở root repo FE bằng Android Studio, đồng bộ Gradle rồi Run module `app`.
5. Bấm **Tiếp tục với Google**, chọn tài khoản Google của mình.
6. Vào **Tài khoản → Hồ sơ thể chất → Thiết lập** để lưu hồ sơ lần đầu.

Địa chỉ BE khác nhau theo nơi chạy UI:

| Nơi chạy UI | Ví dụ địa chỉ BE |
|---|---|
| Android Emulator trên máy chạy BE | `http://10.0.2.2:8000/` |
| Điện thoại thật | `http://IP-LAN-MAY-CHAY-BE:8000/`, thay bằng IP thật, cùng mạng |
| React Admin trên chính máy chạy BE | `http://localhost:8000` |

### 2.2. Các nút ở thanh dưới Android

| Nút | Nội dung |
|---|---|
| **Trang chủ** | Tổng ngày, calo, dinh dưỡng, nước và tác vụ nhanh |
| **Nhật ký** | Món ăn, vận động, nước, ghi chú/tâm trạng của ngày đang chọn |
| **+** | Mở **Thêm bữa ăn**, chọn nhập món thủ công hoặc chụp ảnh |
| **Thực đơn** | Các bữa ăn đã lưu, thêm nhanh và tạo bữa từ nhật ký |
| **Tài khoản** | Hồ sơ, BMI/TDEE, các lối vào tính năng, chat, lịch, đăng xuất |

Trong **Tài khoản**, cuộn xuống để thấy các ô tiện ích: **Chụp món ăn, Tìm món ăn,
Dinh dưỡng, Vận động, Uống nước, Cân nặng, Tư vấn bữa ăn, Lịch sử ngày**.

Ngày chọn ở Trang chủ/Nhật ký/Thực đơn/lịch được dùng chung cho các log liên quan.
Trước khi thêm dữ liệu, kiểm tra ngày đang chọn. Màn Cân nặng có ngày đo riêng.

## 3. Đăng nhập và mở Admin Android / React Admin web — task 5, 11, 16

### 3.1. Chuẩn bị tài khoản Admin: giáo viên thực hiện

1. Tài khoản muốn làm Admin cần **đăng nhập Google trên Android ít nhất một lần**
   với BE đang dùng, để tài khoản tồn tại trong database.
2. Mở **Terminal mới** trong root repo `App_Heathy_BE`. Dùng cùng môi trường Python
   và cùng cấu hình database với BE đang phục vụ app. Giữ Terminal server đang chạy.
3. Chạy lệnh sau, **thay email ví dụ bằng email Google thực tế**:

   ```powershell
   python -m app.admin_setup --email "email-google-cua-ban@gmail.com"
   ```

   Nếu repo dùng `.venv` và chưa kích hoạt, có thể dùng trực tiếp:

   ```powershell
   .\.venv\Scripts\python.exe -m app.admin_setup --email "email-google-cua-ban@gmail.com"
   ```

   Dòng trên áp dụng khi môi trường Python của máy thực sự nằm ở `.venv`.

4. Thành công sẽ có thông báo dạng:

   ```text
   Admin granted to user ID ... Sign in again in React Admin.
   ```

Lệnh chỉ cấp quyền cho tài khoản Google **đã tồn tại, đang hoạt động**. Nếu báo
`Sign in with Google first using an active account`, kiểm tra email, trạng thái
tài khoản và việc Terminal đang dùng đúng database. Đăng nhập Google không tự cấp Admin.

### 3.1.1. Mở Admin ngay trên Android

1. Sau khi được cấp quyền, đăng xuất và đăng nhập lại Google trên app Android.
   Cổng **Quản trị AppHealth** mở với tài khoản có role `admin` từ BE.
2. Nếu đang ở giao diện sức khỏe: **Tài khoản → Quản trị viên → Mở quản trị**.
   Nếu vừa được đổi role trong DB, bấm **Tải lại** ở Tài khoản để cập nhật quyền.
3. Chọn **Tổng quan**, **Người dùng**, **Thực phẩm**, **Bài tập** hoặc **Lịch sử AI**.
   Các mục đọc dữ liệu thật từ cùng API với web. Có bộ lọc và phân trang trong danh sách.
4. **Về ứng dụng** quay lại giao diện sức khỏe; **Đăng xuất** thoát tài khoản Google
   của ứng dụng. Đăng xuất Android và web là các phiên riêng của cùng tài khoản.
5. Khi quyền bị thu hồi, BE từ chối yêu cầu quản trị. App loại dữ liệu Admin khỏi
   giao diện khi nhận từ chối/xác nhận role mới; tài khoản không có role Admin không
   được cấp quyền bằng việc đổi email hiển thị hay tài khoản mẫu.

Android và web chia sẻ dữ liệu qua BE. Ví dụ: sửa thực phẩm trên web, sau đó bấm
**Tải lại** ở danh sách Android để xem thay đổi, hoặc làm theo chiều ngược lại.
Hai UI không tự cập nhật thời gian thực từ màn hình của nhau.

### 3.2. Cấu hình trang web Admin: thực hiện lần đầu trên mỗi máy

1. Mở Terminal ở thư mục **`admin-web` của repo FE**. Máy cần Node.js theo
   [README FE](../README.md) (Node 22.12+ hoặc 24).
2. Nếu chưa có `.env.local`, sao chép `.env.example` thành `.env.local`.
   Mở file bằng trình soạn thảo và điền:

   ```dotenv
   VITE_API_BASE_URL=http://localhost:8000
   VITE_GOOGLE_WEB_CLIENT_ID=WEB_CLIENT_ID_THAT.apps.googleusercontent.com
   ```

   - Thay toàn bộ Client ID ví dụ bằng **OAuth Client ID loại Web application**,
     dùng cùng giá trị với `GOOGLE_WEB_CLIENT_ID` của BE.
   - Nếu BE chạy ở máy khác, thay URL bằng địa chỉ BE truy cập được từ trình duyệt.
   - URL BE là địa chỉ gốc, không thêm `/api/v1`.
3. Trong Google Cloud, mở **Web OAuth client → Authorized JavaScript origins**,
   thêm origin trang Admin:

   ```text
   http://127.0.0.1:5173
   ```

   Nếu cũng sử dụng `http://localhost:5173`, thêm origin đó riêng.
4. Trong `.env` của BE, `CORS_ORIGINS` phải cho phép origin tương ứng, ví dụ:

   ```dotenv
   CORS_ORIGINS=["http://localhost:5173","http://127.0.0.1:5173"]
   ```

   Khởi động lại BE sau khi thay cấu hình. Giữ các cấu hình database và key đang dùng.

### 3.3. Chạy và đăng nhập Admin

1. Giữ BE đang chạy. Trong Terminal tại `admin-web`, chạy:

   ```powershell
   npm ci
   npm run dev
   ```

   `npm ci` dùng khi cài lần đầu hoặc sau khi dependencies thay đổi. Những lần sau
   nếu thư viện đã đầy đủ thì chỉ cần `npm run dev`.
2. Giữ Terminal này mở. Trên trình duyệt của máy đó, vào:

   **<http://127.0.0.1:5173>**
3. Tại **Cổng quản trị**, bấm đăng nhập Google, chọn **đúng email đã cấp Admin**.
4. Nếu đã mở phiên trước khi cấp quyền, đăng xuất rồi đăng nhập lại.
5. Đăng nhập thành công sẽ thấy trang **Tổng quan AppHealth** và menu quản trị.
   Nếu menu bị thu gọn, bấm biểu tượng **☰** để mở.

Nếu Terminal báo cổng 5173 đang được dùng, kiểm tra phiên Admin đã chạy trước đó.
Source hiện đặt cố định cổng 5173; cần xử lý phiên cũ hoặc cấu hình cổng/origin
nhất quán trước khi đăng nhập. Sửa `.env.local` xong cần khởi động lại `npm run dev`.

### 3.4. Mở đúng phần cho từng task Admin

| Task gốc | Sprint | Đường vào Admin Android hoặc web | Thao tác thực hành |
|---|---|---|---|
| **5** | S1-T05 | Android: **Mở quản trị → Người dùng**. Web: Google → menu **Người dùng** | Xem danh sách, tìm tên/email, lọc trạng thái và sửa trạng thái tài khoản |
| **11** | S2-T06 | Android hoặc web: **Thực phẩm/Bài tập** | Tìm/lọc danh mục, thêm/sửa/ngừng sử dụng mục và lưu về cùng BE |
| **16** | S3-T04 | Android: **Tổng quan/Lịch sử AI**. Web: **Dashboard/Lịch sử AI** | Chọn khoảng ngày, **Xem báo cáo**, xem biểu đồ và chi tiết các bản ghi AI |

Một số nút mặc định của React Admin có thể hiện tiếng Anh, ví dụ **Create** (Thêm),
**Save** (Lưu), **Add filter** (Thêm bộ lọc). Tên các mục menu chính ở bảng trên dùng tiếng Việt.

**Task 11 — chi tiết danh mục:**

- **Thực phẩm:** nhập tên, calo/100 g, Protein/100 g, Carb/100 g, Fat/100 g và trạng thái sử dụng.
- **Bài tập:** nhập tên, MET và trạng thái sử dụng.
- Trong màn sửa, **Ngừng sử dụng** đưa mục ra khỏi danh mục đang dùng; nhật ký đã
  ghi trước đó vẫn được giữ. Nên tạo mục thực hành riêng để demo sửa/ngừng sử dụng.

**Task 16 — chi tiết biểu đồ và AI logs:**

- Dashboard: đặt **Từ ngày**, **Đến ngày** rồi **Xem báo cáo**. Dữ liệu gồm người dùng,
  lượt ghi món, vận động, nước và yêu cầu AI; chọn khoảng thời gian có hoạt động.
- **Lịch sử AI:** chọn khoảng ngày; dùng **Add filter** nếu cần lọc **ID người dùng**
  hoặc **Loại** (`Chat`/`Ảnh`). Bấm một dòng để xem câu hỏi, trạng thái, mã lỗi và phản hồi.
- Có thể tạo dữ liệu demo bằng cách dùng Android gửi chat hoặc phân tích ảnh trước,
  rồi quay lại trang **Lịch sử AI** và cập nhật danh sách. Provider chưa hoạt động
  có thể tạo bản ghi lỗi thay vì phản hồi thành công.

## 4. Vào chat AI và nhắn tin — task 12 / S3-T01

**Đường vào: Android → Tài khoản → cuộn xuống → Tư vấn bữa ăn.**

Ô này có biểu tượng bong bóng chat và dòng phụ **Hỏi đáp dinh dưỡng**. Nó nằm trong
nhóm tiện ích, không phải một tab riêng ở thanh điều hướng dưới.

1. Đăng nhập Google và lưu **Hồ sơ thể chất** trước. Nếu chưa có hồ sơ, làm task 1.
2. Nếu muốn tư vấn theo ngày khác, chọn ngày ở Trang chủ hoặc lịch trước khi vào chat.
3. Mở **Tư vấn bữa ăn**, kiểm tra dòng **Tư vấn theo ngày ...**.
4. Nhập câu hỏi vào ô **Nhắn tin tư vấn**, ví dụ: `Gợi ý một bữa tối phù hợp với ngày hôm nay`.
5. Bấm **Gửi**, chờ câu trả lời. Câu hỏi hiện phía người dùng, phản hồi hiện phía AI.
6. Nhập câu hỏi tiếp, ví dụ: `Cho tôi chi tiết bữa trưa vừa gợi ý`, rồi bấm **Gửi**.
7. Rời màn rồi mở lại để xem lịch sử theo tài khoản. Khi có nhiều lịch sử, dùng
   **Tải tin nhắn cũ hơn** nếu nút này xuất hiện.

**Cách thực đơn và các nút hỏi tiếp hoạt động:**

- Mỗi tin nhắn AI chứa **đầy đủ câu trả lời và các bữa ăn gợi ý**, cuộn cùng lịch sử.
  Các chi tiết gợi ý trong lịch sử cũ cũng được hiển thị với đúng tin nhắn của chúng.
- Phần **Bạn có thể hỏi tiếp** phía trên ô soạn chỉ chứa câu hỏi ngắn, ví dụ
  **Chi tiết bữa trưa**, **Đổi sang món chay**. Có thể cuộn ngang để xem các nút.
- Bấm nút sẽ **điền sẵn vào ô nhập**. Có thể sửa nội dung; bấm **Gửi** mới gửi tin nhắn.
- Khi tải tin nhắn cũ hơn, màn hình giữ vị trí trong lịch sử thay vì đưa về tin mới nhất.
- Lịch sử chat gắn với tài khoản. Ngày chọn cung cấp ngữ cảnh tư vấn, không tạo
  một phòng chat riêng cho mỗi ngày. Chat không tự ghi món vào nhật ký.

BE cần cấu hình Groq/key/model hoạt động để nhận trả lời AI thật. Đăng nhập Google
thành công chưa có nghĩa provider AI đã được cấu hình. Khi chat báo lỗi, đối chiếu
thông báo BE; Admin có thể xem bản ghi tại task 16.

## 5. Bảng đường vào đầy đủ theo số task gốc

| Task | Sprint | Tính năng | Vào UI hiện tại |
|---|---|---|---|
| **1** | S1-T01 | Hồ sơ, BMI/TDEE | Android → **Tài khoản → Hồ sơ thể chất → Thiết lập/Chỉnh sửa** |
| **2** | S1-T02 | Tổng ngày và tiến độ calo | Android → **Trang chủ → Calo còn lại** |
| **3** | S1-T03 | Camera và upload ảnh | Android → **Trang chủ → Chụp món ăn**; hoặc **+ → Chụp ảnh món ăn** |
| **4** | S1-T04 | Tìm món và chỉnh khẩu phần | Android → **Trang chủ → Tìm món ăn**; hoặc **+ → Chọn món ăn thủ công** |
| **5** | S1-T05 | Admin login/layout/người dùng | Android: **Mở quản trị → Người dùng**; hoặc web React Admin, theo mục 3 |
| **6** | S2-T01 | Thanh dinh dưỡng và biểu đồ macro | Android → **Trang chủ → Dinh dưỡng**, hoặc **Tổng quan dinh dưỡng → Chi tiết** |
| **7** | S2-T02 | Loại vận động và thời gian | Android → **Trang chủ → Vận động** |
| **8** | S2-T03 | Uống nước và tiến độ ly | Android → **Trang chủ → Uống nước** |
| **9** | S2-T04 | Bữa ăn đã lưu và thêm nhanh | Android → tab **Thực đơn** |
| **10** | S2-T05 | Nhập cân nặng và biểu đồ tháng | Android → **Trang chủ → Cân nặng** |
| **11** | S2-T06 | Admin quản lý thực phẩm/bài tập | Admin Android hoặc web → **Thực phẩm/Bài tập**, theo mục 3 |
| **12** | S3-T01 | Chat và gợi ý | Android → **Tài khoản → Tư vấn bữa ăn**, theo mục 4 |
| **14** | S3-T02 | Nhật ký tổng hợp và chỉnh/xóa | Android → tab **Nhật ký** |
| **15** | S3-T03 | Lịch và chọn ngày quá khứ | Android → **Tài khoản → Lịch sử ngày** |
| **16** | S3-T04 | Admin biểu đồ và lịch sử AI | Admin Android → **Tổng quan/Lịch sử AI**, hoặc web → **Dashboard/Lịch sử AI** |

Các ô **Chụp món ăn, Tìm món ăn, Dinh dưỡng, Vận động, Uống nước, Cân nặng**
cũng có trong **Tài khoản**; cuộn xuống để thấy.

## 6. Thực hành các task Android còn lại

### Task 1 — Hồ sơ và BMI/TDEE

1. Vào **Tài khoản → Hồ sơ thể chất**.
2. Bấm **Thiết lập** khi chưa có hồ sơ, hoặc **Chỉnh sửa** khi đã có.
3. Chọn ngày sinh, giới tính; nhập chiều cao bằng **cm**, cân nặng bằng **kg**;
   chọn mức vận động và mục tiêu.
4. Khi nhập hợp lệ, hộp thoại hiện kết quả tính trước khi lưu. Bấm **Lưu hồ sơ**.
5. Kiểm tra BMI/TDEE trong **Chỉ số sức khỏe nhanh**; rời màn rồi mở lại để đối chiếu.

### Task 2 — Tiến độ calo trong ngày

1. Mở **Trang chủ**, chọn đúng ngày trên lịch tuần.
2. Xem **Calo còn lại**, mục tiêu, **Tiến độ đã ăn**, tổng **Đã ăn** và **Vận động**.
3. Thêm một món bằng task 4, quay lại Trang chủ để kiểm tra tổng thay đổi.

Thanh tiến độ = lượng **đã ăn / mục tiêu**. Calo còn lại = **mục tiêu − đã ăn + vận động**.
Ví dụ minh họa: mục tiêu 1.800, đã ăn 1.180, vận động 120 thì còn lại **740 kcal**.
Số liệu trên app lấy từ BE, không cố định theo hình thiết kế.

### Task 3 — Camera, chọn ảnh và ghi món

1. Vào **Chụp món ăn** hoặc **+ → Chụp ảnh món ăn**.
2. Cấp quyền camera, bấm **Chụp ảnh**; cũng có thể bấm **Chọn ảnh** để dùng thư viện.
3. Kiểm tra ảnh xem trước, bấm **Phân tích ảnh** và chờ AI trả kết quả.
4. Xem/sửa tên món, khối lượng, calo và các chất của khẩu phần; chọn bữa và giờ ăn.
5. Bấm **Xác nhận và lưu nhật ký**. Vào **Nhật ký** đúng ngày để xem món vừa ghi.

Chỉ phân tích ảnh chưa tạo food log. Dữ liệu được ghi sau bước xác nhận. Khi emulator
không có camera dùng được, chọn ảnh từ thư viện để thực hành luồng upload/phân tích.

### Task 4 — Tìm món và chỉnh khẩu phần

1. Vào **Tìm món ăn** hoặc **+ → Chọn món ăn thủ công**.
2. Gõ tên thực phẩm, đợi danh sách rồi chọn một món có trong danh mục BE.
3. Sửa **Khẩu phần (gram)** để xem calo/Protein/Carb/Fat được tính lại.
4. Chọn bữa, nhập giờ ăn theo `HH:mm`, bấm **Thêm vào nhật ký**.
5. App về Trang chủ; kiểm tra tiến độ và mở **Nhật ký** để đối chiếu món.

### Task 6 — Dinh dưỡng và tỷ lệ macro

1. Vào **Dinh dưỡng** hoặc bấm **Chi tiết** trên thẻ **Tổng quan dinh dưỡng** ở Trang chủ.
2. Chọn ngày, xem thanh Protein/Carb/Fat và biểu đồ tỷ lệ năng lượng.
3. Ở **Tùy chỉnh mục tiêu**, nhập các mục tiêu muốn dùng; tổng phần trăm ba chất
   phải bằng **100%** để lưu. Cân nặng mục tiêu là trường không bắt buộc.
4. Bấm **Lưu mục tiêu**, kiểm tra lại thanh/biểu đồ với số liệu đã ghi trong ngày.

### Task 7 — Vận động và thời gian

1. Vào **Vận động**, chọn ngày và **Loại vận động** trong danh sách.
2. Nhập **Thời gian hoạt động (phút)**. Màn hình hiện calo ước tính khi hồ sơ và dữ liệu hợp lệ.
3. Bấm **Lưu vận động**; mở Trang chủ/Nhật ký cùng ngày để xem kết quả.

Nếu danh sách trống, cần có bài tập đang sử dụng trong BE; có thể tạo qua task 11.

### Task 8 — Nước và số ly

1. Vào **Uống nước**, chọn ngày và xem tổng ml, mục tiêu và số ly.
2. Bấm **+ / −** để cộng/trừ theo dung tích ly đang cài.
3. Ở **Mục tiêu uống nước**, nhập mục tiêu tùy chỉnh hoặc để trống để app tự tính;
   chọn dung tích ly rồi bấm **Lưu cài đặt nước**.
4. Quay lại Trang chủ/Nhật ký để đối chiếu tổng nước cùng ngày. Hai màn này cũng có nút cộng/trừ nhanh.

### Task 9 — Thực đơn đã lưu và thêm nhanh

1. Thêm ít nhất một món vào nhật ký bằng task 3 hoặc 4.
2. Mở tab **Thực đơn**, chọn đúng ngày có món.
3. Ở **Lưu bữa từ nhật ký ngày đã chọn**, nhập **Tên bữa ăn**, chọn các món,
   bấm **Lưu bữa ăn**. Mỗi bữa tối đa 30 món.
4. Bữa vừa lưu xuất hiện trong danh sách. Có thể **Sửa** tên/khẩu phần hoặc **Xóa** bữa đã lưu.
5. Chọn ngày muốn ghi, chọn **bữa ăn và giờ ăn** phía trên; tại bữa đã lưu bấm
   **Thêm nhanh vào nhật ký**.
6. Mở tab **Nhật ký**, kiểm tra món ở đúng ngày/bữa/giờ vừa chọn.

### Task 10 — Cân nặng theo tháng và chỉnh số đo cũ

1. Vào **Cân nặng**, dùng mũi tên ở **Xu hướng theo tháng** để đổi tháng.
2. Tại **Ghi số đo**, bấm **Ngày đo** để chọn ngày, nhập cân nặng rồi **Lưu số đo**.
3. Xem biểu đồ và danh sách của tháng chứa ngày vừa ghi.
4. Bấm biểu tượng **Sửa** cạnh một số đo cũ, kiểm tra ngày trong hộp thoại,
   nhập giá trị mới rồi **Lưu thay đổi**.
5. Bấm biểu tượng **Xóa** và xác nhận để xóa số đo của ngày đó.

Có thể thêm/sửa/xóa số đo quá khứ; ngày tương lai bị chặn. Nhập tối đa hai chữ số
thập phân. Hồ sơ cần giữ ít nhất một số đo, nên thêm số đo khác trước khi xóa bản
ghi cuối. Nhập lại ngày đã có số đo sẽ cập nhật bản ghi của ngày đó.

### Task 14 — Nhật ký tổng hợp, sửa/xóa và ghi chú

1. Mở tab **Nhật ký**, chọn đúng ngày.
2. Xem các nhóm **Món ăn**, **Vận động**, **Nước uống**, **Ghi chú & tâm trạng**.
3. Tại món ăn/vận động, bấm **Sửa** để chỉnh khẩu phần/thời gian, hoặc **Xóa** rồi xác nhận.
4. Ở **Nước uống**, bấm **Chi tiết** khi đã có log để mở danh sách và sửa/xóa từng lần ghi.
5. Nhập nội dung ghi chú, chọn tâm trạng, bấm **Lưu ghi chú**. Cần có nội dung ghi chú để lưu.
6. Đối chiếu tổng ngày sau thay đổi và mở lại để kiểm tra dữ liệu đã lưu.

### Task 15 — Lịch và ngày quá khứ

1. Vào **Tài khoản → Lịch sử ngày** để mở màn **Lịch sử theo ngày**.
2. Dùng mũi tên để đổi tháng; xem màu ô và chú giải ở dưới lịch.
3. Bấm một ngày quá khứ hoặc hôm nay. App chuyển sang **Nhật ký** của ngày đó.
4. Kiểm tra ngày trên màn trước khi sửa/thêm log. Dùng **Về hôm nay** trên màn lịch
   để quay lại nhật ký ngày hiện tại.

Lịch tuần ở Trang chủ/Nhật ký/Thực đơn cũng cho đổi ngày, nhưng lịch tháng nằm ở lối vào trên.

## 7. Các tình huống thường gặp khi thực hành

| Hiện tượng | Cách xử lý theo bản hiện tại |
|---|---|
| Android đăng nhập được nhưng không thấy mục Admin | Kiểm tra role ở đúng BE/database; vào **Tài khoản → Tải lại** hoặc đăng nhập lại; role Admin sẽ có **Quản trị viên → Mở quản trị** |
| Admin báo tài khoản chưa có quyền | Đăng nhập Android trước; giáo viên chạy lệnh cấp Admin cho đúng email, đúng database; đăng xuất/đăng nhập lại web |
| Lệnh cấp Admin không tìm thấy tài khoản | Kiểm tra email Google đã đăng nhập và cấu hình DB/môi trường Python của Terminal BE |
| Trang web báo Google chưa cấu hình | Điền `VITE_GOOGLE_WEB_CLIENT_ID` trong `admin-web/.env.local`, rồi khởi động lại Vite |
| Google báo lỗi origin | Kiểm tra đúng Web OAuth client và origin `http://127.0.0.1:5173`; đối chiếu thêm CORS của BE |
| Trình duyệt báo không kết nối được Admin | Kiểm tra Terminal `npm run dev` còn chạy; Admin và BE cần hai tiến trình riêng |
| Android không kết nối được BE | Kiểm tra BE đang chạy, URL emulator/điện thoại thật, mạng và `/health` từ thiết bị |
| Nút Gửi trong chat không bấm được | Đợi tải/lưu xong, nhập ít nhất hai ký tự, kiểm tra đã có hồ sơ và xử lý cảnh báo kết quả ghi chưa rõ nếu có |
| Bấm gợi ý chat nhưng chưa có tin mới | Gợi ý chỉ điền vào ô nhập; bấm **Gửi** mới tạo tin nhắn |
| Chat/phân tích ảnh báo lỗi AI | Kiểm tra cấu hình Groq/key/model ở BE; xem log lỗi BE hoặc **Lịch sử AI** trên Admin |
| Danh mục món/bài tập trống | Kiểm tra mục đang sử dụng ở task 11; tìm theo tên có trong BE |
| Dashboard/log AI chưa có dữ liệu | Tạo hoạt động trên Android, chọn đúng khoảng ngày/bộ lọc rồi cập nhật danh sách/báo cáo |
| Mất kết nối trong khi ghi | Bấm **Tải lại**, kiểm tra nhật ký trước khi gửi lại để tránh ghi trùng |

## 8. Thứ tự demo ngắn cho lớp

1. Google login → task **1**: lưu hồ sơ.
2. Task **4**: thêm món → task **2/6**: xem calo và dinh dưỡng.
3. Task **7/8**: ghi vận động và nước → task **14**: xem/sửa nhật ký.
4. Task **9**: lưu bữa và thêm nhanh.
5. Task **10/15**: nhập số đo quá khứ, xem biểu đồ tháng và lịch sử theo ngày.
6. Task **3/12**: phân tích ảnh và chat khi provider AI đã cấu hình; kiểm tra thực đơn
   nằm trong tin AI và các nút hỏi tiếp chỉ điền sẵn vào ô soạn.
7. Trên Android hoặc trình duyệt: task **5** đăng nhập Admin/xem người dùng → task **11**
   quản lý danh mục → task **16** xem biểu đồ và lịch sử AI. Sửa danh mục ở một UI,
   tải lại UI còn lại để đối chiếu dùng chung dữ liệu.

Khi chụp minh chứng báo cáo, ghi rõ số task gốc, tài khoản thực hành, ngày dữ liệu
và thao tác đã thực hiện. Bảng đường vào ở mục 5 dùng để đối chiếu đủ 15 task trong danh sách gốc.
