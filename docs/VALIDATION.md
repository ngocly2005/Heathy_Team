# Kiểm chứng tích hợp

## 2026-11-14 — Giao diện Admin web theo ảnh FitChat

- 12 tests web thành công; TypeScript và production build thành công. Vẫn còn
  cảnh báo bundle lớn, không có lỗi build. Không thêm dependency sản phẩm.
- Kiểm tra trình duyệt cục bộ qua đúng App, authProvider và dataProvider, dùng
  API giả riêng, không inject phiên hoặc tạo lối bỏ qua auth trong sản phẩm.
  Có 34 ảnh của 12 route trên desktop 1440px/mobile 390px, các trạng thái trống
  và lỗi báo cáo. Các bảng cuộn ngang bên trong; không làm tràn toàn trang.
- 15 kiểm tra tương tác thành công: menu rộng/hẹp/mobile, tìm kiếm/lọc/phân
  trang, CSV theo bộ lọc, lưu trạng thái user, từ chối khóa Admin, thực phẩm và
  bài tập create/edit, xác nhận ngừng sử dụng, JSON AI đầy đủ, khoảng ngày,
  bảng số liệu, logout và từ chối tài khoản thường/bị khóa/mật khẩu sai.
- Nút CSV được giới hạn rõ 100 dòng để khớp giới hạn API; mặc định 1.000 của
  React Admin vượt giới hạn. Không thêm endpoint xuất dữ liệu. Không đổi payload
  user status-only hoặc dữ liệu lưu catalog, quyền vẫn xác định qua BE.
- Lượt xem đầu phát hiện bảng/bộ lọc tràn mobile, đã sửa track grid và flex của
  toolbar; bỏ prop end bị truyền xuống DOM (menu đã khớp route gốc chính xác).
  Bộ kiểm tra ban đầu có bước đọc kết quả trước khi lọc/redirect hoàn tất; đã
  đồng bộ theo phản hồi và giao diện, giữ kết quả cũ để đối chiếu.
- Hai phản hồi HTTP 503 trong ảnh lỗi là chủ đích của fixture; không coi là lỗi
  runtime ngoài dự kiến. Google/provider và database triển khai thật chưa được
  kiểm thử trong đợt này. Harness chặn mọi kết nối ra ngoài máy cục bộ.
- Android, BE, migration và chuỗi phát hành cũ giữ nguyên, không chạy lại các bộ
  kiểm tra không liên quan hoặc upgrade database thật. Xem
  [các trang thiết kế và phần bỏ qua](ADMIN_WEB_DESIGN_20261114.md).

## 2026-11-13 — Đối chiếu 17 ảnh Figma, chỉ đổi cách trình bày

- Android: 75 JVM tests thành công, APK và lintDebug thành công; 0 Error/Fatal,
  113 Warning và 2 Hint trong toàn project. Cảnh báo mới `ModifierParameter`
  ở helper giao diện chỉ liên quan thứ tự tham số tùy chọn, không chặn build.
- Rà soát riêng các phần ngày chọn, tiến độ calo, nhập khẩu phần, profile,
  mục tiêu và sửa/xóa nhật ký: giữ API, payload, công thức, validation và các
  chốt busy/loading/uncertain. Giữ đủ Chat AI, Admin theo quyền BE và bảo mật.
- 10 preview Compose dựng thành công và đã xem ảnh: Home ngắn/dài, Diary có
  món/rỗng, Profile có hồ sơ/rỗng, Macro dài, Food, Exercise, Login. Diary rỗng
  được dựng ở 320dp/chữ 150%; các khối đa lượng chuyển dọc và thanh dưới xuống
  dòng. 22 file UI trong fixture trùng từng byte và SHA-256 với source thật.
- Fixture tách khỏi repo sản phẩm, dùng Layoutlib và trạng thái/API mẫu chặn
  mạng. Không thêm dependency render vào app. Ảnh không bao gồm shell bên ngoài,
  dialog hoặc tương tác mở trang con; không nhận là kiểm thử emulator, Google,
  đăng ký/đăng nhập thật hay thao tác lưu/xóa với máy chủ.
- Từ ảnh xuất có thể đối chiếu bố cục/màu/nhóm nội dung, chưa đo trực tiếp từng
  node Figma. Xem [bảng áp dụng và chi tiết bỏ qua](UI_FIGMA_20261113.md).
- Không đổi BE, React Admin web, API hay migration nên không chạy lại bộ BE/web
  trong đợt UI này. Không upgrade database thật. Lượt build đầu phát hiện thiếu
  import Color ở Profile, đã sửa và build lại; đồng thời sửa tiêu đề dinh dưỡng
  để tránh hai nút quay lại trong shell. Bản cuối được kiểm tra lại sau chỉnh sửa.

## 2026-11-13 — Rà soát 32 task, email/mật khẩu song song Google

- Android: 75 JVM tests thành công, APK và lintDebug thành công, không có Error/Fatal.
- BE: 26 HTTP tests thành công, gồm 7 kiểm tra mới về đăng ký, login, đổi mật khẩu,
  liên kết Google, dữ liệu cùng user ID, role Admin và tài khoản bị khóa. Database
  SQLite trong bộ nhớ, Google/Groq được mock; không dùng DB/provider thật.
- Web: 12 tests thành công (5 mới về hai phương thức login, role và thu hồi token
  khi không lưu được phiên), TypeScript và production build thành công. Còn cảnh
  báo bundle lớn có sẵn.
- OpenAPI: 37 paths/55 operations. Đối chiếu route Retrofit Auth/App/Admin không
  có endpoint thiếu ở BE. Một head 20261113_0012; sinh SQL upgrade/downgrade offline
  cho đúng một revision mới, guard downgrade chạy trước thao tác xóa cột. DDL của
  15 bảng biên dịch được cho PostgreSQL; chưa chạy migration thật.
- Giữ Google, thêm email/password Argon2; chỉ liên kết sau xác thực, không tự gộp
  email trùng. Không lưu mật khẩu ở Android/web, không trả hash hoặc input mật
  khẩu trong lỗi validation. API/current-user/refresh chấp nhận cả hai phương thức.
- Đã kiểm tra bảng đối chiếu 32 mã BL, đường vào UI, lịch dự kiến trong sprint và
  giới hạn kiểm thử. Không đổi ngày Git thành ngày kế hoạch.
- Lượt test BE đầu có assertion dùng từ “short” trùng mã lỗi “too_short”; đã đổi
  mẫu test và xác nhận response không chứa input mật khẩu. Bộ tách task có lỗi
  thứ tự khai báo một schema cha; đã sửa exporter và kiểm tra lại từng mốc.
- Chưa kiểm thử email/Google trên emulator, camera, Groq, CORS thực tế hay DB
  PostgreSQL của học sinh. Giáo viên tự cài requirements và upgrade sau khi pull.

## 2026-11-08 — Cấu hình Google web và nhận biết tài khoản

- 7 tests web thành công (5 mới): cấu hình ID-token popup không tự chọn tài khoản,
  Client ID rỗng bị chặn, logout/reject gọi disableAutoSelect nhưng không revoke
  tài khoản Google, lỗi thư viện Google không ngăn app logout, email từ chối rõ ràng.
- TypeScript và production build thành công; còn cảnh báo bundle lớn có sẵn.
- Code cũ đã có `auto_select=false`. Thêm rõ `button_auto_select=false`, popup,
  chặn callback đăng nhập đồng thời, ghi trạng thái Google khi logout/reject và
  hướng dẫn chọn tài khoản khác. Quyền Admin vẫn do BE quyết định, không hard-code email.
- Bản dev hiện origin của tab để cấu hình đúng Web OAuth client. Lỗi trong ảnh
  `no registered origin` cần sửa Authorized JavaScript origins trong Google Cloud;
  thay đổi source không thể tự đăng ký origin và không bảo đảm ép bộ chọn tài khoản.
- Chưa xác nhận Google thật trên máy học sinh hoặc sửa Google Cloud của họ.
  Không thay BE/Android, không thêm migration; không chạy lại các bộ tests không đổi.

## 2026-11-08 — Chat đầy đủ và Admin Android/web song song

- 70 JVM tests thành công, gồm 21 tests mới (7 chat và 14 native Admin).
  `assembleDebug` và `lintDebug` thành công; 0 Error/Fatal, 112 Warning, 2 Hint
  trong toàn project. BE có 19 tests thành công (7 mới); React Admin có 2 tests
  và production build thành công, còn cảnh báo bundle lớn như trước.
- Chat đọc cả payload cũ/mới, giữ mọi bữa ăn dài trong từng tin AI, tách câu hỏi
  tiếp ra khỏi nội dung thực đơn; bấm chip chỉ điền composer. Đã kiểm tra tải thêm
  lịch sử không cuộn về tin mới nhất, cùng ngữ cảnh BE bao gồm bữa ăn đã gợi ý.
- Native Admin dùng cùng API/role/database với React Admin. Kiểm tra quyền live,
  đổi role trong DB khi JWT vẫn còn hạn, từ chối tự nâng role qua form trạng thái,
  thu hồi quyền, lỗi lưu danh tính sau thu hồi, phản hồi cũ/đổi tài khoản và khóa
  gửi tiếp khi chưa rõ kết quả mutation. Không tự gửi lại thao tác ghi.
- Native có Tổng quan/biểu đồ, danh sách người dùng/đổi trạng thái, thực phẩm và
  bài tập CRUD/ngừng dùng, lịch sử AI/chi tiết; Profile có Mở quản trị và lối về
  giao diện sức khỏe. Quyền BE được đọc lại khi tải/ghi và khi trở lại app.
- Đã render 14 preview Compose ngoài repo sản phẩm: xem được 13 bố cục chat,
  nội dung thực đơn đầy đủ, native dashboard/danh sách/bộ lọc/từ chối quyền,
  gồm màn 320dp và chữ 150%. Một preview CatalogEditor AlertDialog ra ảnh trắng
  do host renderer không chụp overlay; chưa xác nhận trực quan popup này. Không
  coi ảnh trắng là lỗi production hoặc coi render pass là kiểm thử tương tác.
- Fixture render dùng 15 file UI trùng SHA256 và 3 hàm UI trích nguyên văn, VM/API
  mẫu không gọi mạng; dependencies render không được thêm vào repo sản phẩm.
- Chưa kiểm thử Google/Groq, camera, trình duyệt web hoặc thao tác trên emulator
  với server thật trong đợt này. Không thêm migration, không upgrade DB thật;
  582 file BE chia sprint giữ nguyên. Tài liệu vào UI đã cập nhật cho cả hai Admin.
- Lượt unit test đầu có một assertion so sánh cả khoảng trắng cuối nội dung mẫu;
  đã đối chiếu chuẩn hóa whitespace của phần hiển thị và sửa expected test.
  Lượt cuối giữ đầy đủ nội dung và toàn bộ 70 tests thành công.

## 2026-11-08 — Tiến độ, số đo và giao diện chính

- Đã đối chiếu commit sửa chuyển màn trước đó: `fad4c075a261e7f68ff36e975879a4de8716a8a4`
  có trên nhánh main của repo chính. Không coi việc có commit là bằng chứng lỗi
  trên máy học sinh đã hết; lần này bổ sung các nhánh còn thiếu khi rà code.
- GET JSON nhỏ bị ngắt giữa body được phục hồi tối đa một lần, có giới hạn
  đọc trước 1 MiB. Body lớn tiếp tục streaming; body không biết độ dài chỉ
  kiểm tra phần đầu có giới hạn. Không retry lỗi JSON/schema, HTTP, TLS, thao tác
  ghi hoặc lỗi lưu token sau refresh. Lỗi parse có thông báo riêng.
- Thành công khi tìm món/tải lịch xóa đúng lỗi đọc cùng màn; không xóa cảnh báo
  kết quả ghi chưa rõ. Nhật ký tự tải mục tiêu/cỡ ly nước khi vào trực tiếp.
- Phản hồi của thao tác ghi không giải mã được cũng được coi là chưa rõ kết quả;
  khóa gửi tiếp đến khi tải đối chiếu thành công, tránh tạo bản ghi trùng.
- Tiến độ calo tính theo lượng đã ăn/mục tiêu, giữ số lẻ. Calo còn lại dùng BE,
  có tính vận động; không sao chép số liệu minh họa. Lưu/xóa món làm mới dashboard.
- Cân nặng có biểu đồ và danh sách theo tháng, trục ngày cố định theo độ dài
  tháng, chọn ngày bằng DatePicker, sửa/xóa ngày quá khứ trong hộp thoại rõ ngày.
  Giữ 2 chữ số thập phân; chặn tương lai và giải thích khi không thể xóa số đo cuối.
- Đổi Home, Diary, Meals và Profile sang trắng/mint, giữ dữ liệu/API và chức năng
  thật. Giữ bốn tab Trang chủ/Nhật ký/Thực đơn/Tài khoản và nút thêm món.
  Không thêm Reports, Shopping, Premium, thông báo hoặc thuộc tính tài khoản giả.
- Dải tuần dùng mốc tuần ổn định; duyệt tuần rồi chọn ngày không cộng độ lệch lần
  hai. Chữ/nút dùng mint đậm để dễ đọc; các vùng nhiều cột đổi bố cục khi chữ lớn.
- 49 JVM tests thành công (32 regression tests mới trong đợt này); BE có 12 HTTP
  tests thành công (3 mới). Bao gồm socket bị cắt giữa JSON, không replay mutation/
  refresh, cảnh báo ghi chưa rõ, thêm/xóa món khi net calo âm, tháng nhuận,
  chuyển ngày/tài khoản và độ chính xác khi chỉnh số đo/khẩu phần.
- `assembleDebug` và `lintDebug` thành công sau lần chỉnh code cuối: 0 Error/Fatal,
  112 Warning và 2 Hint trong toàn project (2 Warning mới là quy ước vị trí tham số
  Modifier ở component dùng chung). Các cảnh báo thư viện cũ vẫn được báo đầy đủ.
- Đã render 12 ảnh Compose: 5 màn Home/Diary/Meals/Profile/Weight, calorie card,
  2 trường hợp Diary/Profile rộng 320dp chữ 150%, và 4 viewport 393×852dp.
  Đã xem ảnh để đối chiếu bố cục và lịch/mascot của Nhật ký. Không thấy chữ/
  card đè nhau hoặc bị cắt trong các ảnh đã kiểm tra. Thử nghiệm dùng bản source
  UI/resources trùng SHA256 với production, VM/API fixture không gọi mạng, footer
  thật, không mở bottom sheet. Bộ render nằm ngoài repo sản phẩm, không thêm
  plugin/dependency hoặc yêu cầu cài đặt cho học sinh. Đây là kiểm tra hiển thị;
  chưa kiểm thử tương tác trên emulator, Google, camera hoặc BE/provider thật.
- Không thêm migration hay upgrade DB. Bộ chia sprint được giữ nguyên.

## 2026-11-06 — Tải dữ liệu khi chuyển màn

- Mỗi màn tự tải các dữ liệu cần dùng; dữ liệu cùng tài khoản/ngày được dùng lại.
  Không tải toàn bộ tính năng khi chỉ mở một màn. Đổi ngày hoặc lưu dữ liệu làm
  mới cache; lỗi đọc cũ không xuất hiện trên màn khác.
- Phản hồi cũ bị loại khi đổi màn, ngày hoặc tài khoản, kể cả quay lại cùng màn.
  Hủy yêu cầu khi điều hướng không hiện thành lỗi mất kết nối.
- GET/HEAD lỗi kết nối trước khi nhận phản hồi được thử thêm tối đa một lần.
  Không tự gửi lại POST/PUT/PATCH/DELETE, lỗi HTTP/chứng chỉ hoặc POST làm mới token.
- 17 unit tests thành công, gồm 13 regression tests mới: chuyển màn và cache,
  hủy/late response, đổi ngày/tài khoản, lưu rồi điều hướng, giữ khóa khi kết quả
  ghi chưa rõ, phân biệt đã lưu với lỗi tải lại, GET bị ngắt và chống gửi trùng.
  Test mạng dùng HTTP server cục bộ có chủ đích ngắt socket sau khi nhận request.
- `assembleDebug` và `lintDebug` thành công; lint có 0 Error/Fatal, 110 Warning
  trong toàn bộ project. Test/build output được đưa ra ngoài OneDrive vì
  thư mục kết quả cũ trong source bị khóa; không thay cấu hình máy của học sinh.
- Chưa xác nhận lại trên emulator/máy học sinh với Google và BE thật; không
  coi các test mạng giả lập là kiểm thử thiết bị hoặc OAuth thật.

## 2026-11-05 — Đã chạy

- Backend: 9 regression tests HTTP thành công trong venv mới cài đúng dependencies
  repo; SQLite trong bộ nhớ, Google/Groq được mock.
- Android: build `app-debug.apk`, 4 unit tests thành công (1 có sẵn, 3 mới).
  `lintDebug` thành công, không có Error; vẫn còn Warning/Hint trong báo cáo.
- React Admin: TypeScript/production build và 2 test URL/filter thành công.
  Npm audit không còn vulnerability tại thời điểm kiểm tra.
- Alembic: một head `20261105_0011`, sinh SQL PostgreSQL offline; không upgrade DB thật.
- Bộ FE theo sprint: checksum, patch và phạm vi Git được kiểm tra bằng script
  trong `trien_khai_git_fe`; kết quả ở `verification.json` của bộ đó.

BE tests kiểm tra quyền/ownership, Google exchange, refresh/revoke, hồ sơ/mục tiêu,
snapshot khi đổi catalog, food/activity/water, saved meals, weight/diary/calendar,
phân tích ảnh không ghi rồi xác nhận giá trị đã sửa, AI history và ngày UTC+7.

## 2026-11-05 — Chưa chạy và giới hạn

- Google thật, ảnh/chat Groq thật: cần Client ID, SHA-1, key và model giáo viên.
- PostgreSQL upgrade, Docker và bootstrap Admin thật: giáo viên tự chạy theo README BE.
- Chưa kiểm tra UI/camera trên thiết bị hoặc emulator với tài khoản thật.
- Đã thử skill browser và Chrome nhưng không kết nối được bridge/debugger.
  Không nhận rằng đã xem screenshot hoặc thao tác UI React bằng trình duyệt.
- Vite có cảnh báo bundle lớn; lint Android và thư viện test BE còn cảnh báo.
- Bản cuối được build/test; không nhận mọi checkpoint đã qua kiểm tra trên thiết bị.

Trước khi ghi Done trong báo cáo lớp, kiểm tra Google login, lưu/đọc lại trên BE
thật, đổi tài khoản/ngày, camera/AI thật, Admin CRUD/charts/logs và mất kết nối khi ghi.
