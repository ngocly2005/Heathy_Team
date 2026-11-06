# Thiết kế tích hợp Android, React Admin và backend theo sprint

Ngày: 2026-11-05 (UTC+7).

Người dùng đã yêu cầu triển khai và bỏ qua các bước duyệt tiếp của skill brainstorming.
Trạng thái hiện tại xem README và docs/VALIDATION.md; đây là bản thiết kế ban đầu.

Phương án tích hợp đã được người dùng duyệt trong chat, gồm Google Sign-in và
React Admin tại `Heathy_Team/admin-web`. Tài liệu này cụ thể hóa phương án để
người dùng xem trước khi lập kế hoạch thực thi. Đây là thiết kế, chưa phải báo
cáo tính năng đã triển khai hay đã kiểm thử.

## 1. Mục tiêu và ranh giới repository

Bản hoàn chỉnh để giáo viên push và học sinh pull nằm trong hai repository độc lập:

- `code_chinh/Heathy_Team`: Android Kotlin/Compose và `admin-web` React.
  Remote: `https://github.com/ngocly2005/Heathy_Team.git`.
- `code_chinh/App_Heathy_BE`: FastAPI, SQLAlchemy và Alembic.
  Remote: `https://github.com/ngocly2005/App_Heathy_BE.git`.

`trien_khai_git_fe` chứa bộ FE cộng dồn theo task và lịch sử commit cục bộ để sau
này đưa vào `UI/` của `https://github.com/ngocly2005/App_Heathy_Team.git`.
Giữ nguyên `trien_khai_git_be`, các bản bài giảng, checkpoint và bản BE tham chiếu.
Không biến thư mục làm việc cha thành một repository chung cho hai bản chính.
Không push, sửa lịch sử remote hoặc đổi ngày commit trong đợt triển khai này.
Không chạy Docker hay upgrade database đang dùng; giáo viên tự chạy upgrade sau.

Danh sách người dùng có 15 task: 1–12 và 14–16. Giữ nguyên số gốc, không thêm task 13.

## 2. Kết quả rà soát source làm cơ sở

- FE bắt đầu ở `fc2ef06`, BE bắt đầu ở `2bcf4fe`; cả hai working tree sạch trước
  khi viết tài liệu này. Đây là mốc local đã kiểm tra, không khẳng định là remote mới nhất.
- Android có các màn hình sức khỏe, nhật ký, thực đơn và Admin, nhưng dữ liệu chính
  được lưu bằng SharedPreferences/DataStore và catalog tĩnh. Chưa có lớp gọi API
  trong bản `code_chinh/Heathy_Team`; manifest chưa có quyền Internet.
- Đăng nhập Android kiểm tra mật khẩu local và tài khoản mẫu. BE chỉ chấp nhận
  Google ID token đã xác minh rồi cấp access/refresh token ứng dụng.
- Route `camera_capture` được gọi nhưng chưa có destination; chưa có màn chat người dùng.
- BE có profile, daily dashboard, macro, catalog, food/activity/water logs, saved
  meals, weight, diary, calendar và AI. Food/activity logs chưa có API sửa.
- BE chưa có role Admin, Admin CRUD, analytics hoặc bảng lưu tương tác AI.
- BE bản chính chưa có WaterSettings như một số checkpoint cũ. Không chép migration
  từ các checkpoint hoặc từ bản BE chia sprint vào bản chính.
- Có hai file tạo refresh_tokens, một file đuôi `.PY` và một file `.py`; chuỗi
  profile hiện tham chiếu revision `8da74fd2c368`. Phải kiểm tra cách Alembic nạp
  graph và xác định head thực tế trước khi thêm revision mới; không tự nối cả hai
  file như hai lần tạo bảng và không tự sửa revision đã dùng trên database.
- Checkout phát hành đội đã lưu trong máy có thư mục `BackEnd/` và `FrontEnd/`.
  FE mới dùng đúng `UI/` theo yêu cầu; không đổi tên hoặc ghi đè hai thư mục cũ.

## 3. Kiến trúc được chọn

Giữ dự án Kotlin/Compose hiện có và FastAPI hiện có. Bổ sung lớp truy cập dữ liệu
Android theo hướng API → repository → ViewModel → màn hình. Không tạo lại app,
không thêm hệ thống đồng bộ offline, multi-module hoặc một backend riêng cho Admin.

Server là nguồn dữ liệu chính cho các tính năng trong backlog. Local chỉ giữ phiên,
trạng thái giao diện và các tiện ích ngoài backlog đã có. Không tự đẩy nhật ký local
của tài khoản demo lên tài khoản Google; không xóa dữ liệu local cũ.

Hai hướng đã cân nhắc là viết lại UI hoặc dùng nguyên checkpoint tích hợp cũ.
Viết lại làm mất giao diện học sinh đang dùng; checkpoint cũ không chứa các thay đổi
UI/Admin mới. Vì vậy tiếp tục từ hai repository `code_chinh` và chỉ tái sử dụng
đoạn tích hợp cũ sau khi đối chiếu contract, không thay toàn bộ source hiện tại.

## 4. Xác thực và cấu hình kết nối

Android dùng Google Credential Manager để lấy ID token, gửi `/api/v1/auth/google`,
đọc `/auth/me`, và lưu access/refresh token bằng Android Keystore. Email là thông
tin hiển thị, không phải bằng chứng đăng nhập hay quyền Admin. Không giữ lối vào
qua mật khẩu mẫu. Đăng xuất thu hồi refresh token, xóa phiên và dữ liệu trong bộ nhớ.

Lớp HTTP dùng Retrofit/OkHttp, cấu hình origin URL có dấu `/` cuối, gọi `/health`
ở origin và nghiệp vụ dưới `/api/v1/`. Có ví dụ riêng cho emulator và máy thật.
HTTP không mã hóa chỉ được bật ở bản debug. Client ID và URL được cấu hình cục bộ;
không commit `.env`, khóa Groq, token, SDK path hoặc cấu hình máy cá nhân.

Refresh được tuần tự hóa và mỗi request chỉ thử refresh một lần khi gặp 401.
Không tự phát lại các thao tác POST khi lỗi kết nối. Đổi tài khoản hoặc ngày phải
hủy/bỏ kết quả request cũ, không để dữ liệu của phiên trước ghi lên phiên mới.
Lỗi mạng khi refresh không tự được hiểu là token đã bị thu hồi.

Thêm `User.role` với mặc định `user`, giá trị hợp lệ `user`/`admin`. Chỉ BE quyết
định quyền; `/auth/me` trả role. Cung cấp lệnh bootstrap để giáo viên cấp quyền
cho một tài khoản Google đã có, không tự chạy lệnh và không gán Admin qua email
hard-code hoặc dữ liệu do client gửi lên. Mọi API Admin kiểm tra role và status.

React Admin dùng Google Sign-in và cùng cơ chế JWT/refresh. Cấu hình CORS cho
các origin được khai báo, không dùng wildcard để mở quyền tùy ý. Phiên Admin
không dùng chung nơi lưu dữ liệu với Android. Khi bị 401/403, hiển thị luồng đăng
nhập hoặc thông báo quyền phù hợp. Các màn Admin Kotlin cũ không còn là luồng
quản trị chính và không được mở bằng tài khoản mẫu.

## 5. Hợp đồng dữ liệu và hành vi Android

### Hồ sơ, mục tiêu, tổng quan và macro

Form hồ sơ dùng ngày sinh thật, giới tính, chiều cao, cân nặng, mức vận động và
mục tiêu. Hiển thị BMI/BMR/TDEE tính trước khi lưu; sau khi lưu dùng kết quả BE.
Đồng nhất mã enum, đơn vị và công thức hai bên; xử lý mức vận động rất nhiều đang
có trong UI bằng mã BE tương ứng và hệ số 1.9. Không âm thầm đổi ngày sinh từ tuổi.

Thêm cấu hình mục tiêu dinh dưỡng theo user cho calo tùy chỉnh, tỷ lệ protein/carb/fat
và cân nặng mục tiêu. Tỷ lệ hợp lệ phải cộng đủ 100; phần trăm năng lượng macro
đã ăn tính theo 4/4/9. Mục tiêu hiệu lực và tổng ngày được BE trả thống nhất để
Home, Diary, chat và các biểu đồ không dùng các công thức khác nhau. Giữ chính
sách calo mục tiêu hiện có khi chưa có tùy chỉnh; không tự thêm mức ăn kiêng mới.

### Ngày được chọn, nhật ký và lịch

Dùng một selectedDate trong ViewModel dùng chung cho Home, Diary, camera, quick-add,
vận động và nước. Gửi ngày ISO rõ ràng trong mọi thao tác, không để màn hình phụ tự
chuyển về hôm nay. Lịch tải marker theo tháng từ BE; chọn ngày tải chi tiết ngày đó.

Giữ bố cục nhật ký theo giờ bằng trường thời gian bữa ăn tùy chọn trên FoodLog;
record cũ không có giờ được nhóm theo meal_type, không dựng giờ giả từ created_at.
Danh sách lưu ID log của BE để sửa/xóa đúng record dù có hai món giống nhau.
Thêm API sửa food/activity/water log có kiểm tra ownership. Thay đổi số gram của
food log dùng snapshot dinh dưỡng đã lưu, không lấy giá trị catalog mới để đổi
lịch sử. Diary tổng hợp thực phẩm, vận động, nước và ghi chú theo cùng ngày.

Khi đang ghi, khóa nút liên quan. Chỉ coi thao tác thành công khi server xác nhận;
sau đó tải lại tổng và danh sách liên quan. Nếu kết nối mất sau khi gửi, thông báo
kết quả chưa rõ và yêu cầu tải lại trước khi gửi lần nữa, không tự cộng dữ liệu
local hay khẳng định thao tác chưa được lưu.

### Thực phẩm, ảnh, vận động, nước, saved meals và cân nặng

- Tìm thực phẩm theo API với debounce, có trạng thái tải/rỗng/lỗi. Nhập gram/khẩu
  phần hợp lệ, xem dinh dưỡng theo lượng, chọn bữa/giờ/ngày trước khi ghi.
- Camera có preview trong app, xin quyền camera, chụp ảnh hoặc chọn ảnh hệ thống,
  xem lại rồi chủ động upload. BE giữ giới hạn JPEG/PNG/WebP và dung lượng ảnh.
  Phân tích không ghi nhật ký. Người dùng sửa từng kết quả rồi bấm xác nhận để
  tạo log; khóa xác nhận sau thành công hoặc khi kết quả ghi chưa rõ.
- Vận động lấy catalog BE, chọn loại và thời lượng phút, hiển thị năng lượng tính
  theo MET/cân nặng. Không gán ID catalog theo tên hoặc thứ tự list tĩnh.
- Nước dùng WaterSettings theo user: target tự động theo công thức UI đang có hoặc
  target tùy chỉnh, serving 200/500 ml. API cộng/trừ theo ngày bảo đảm tổng không
  âm và xử lý ghi đồng thời; tiến độ ly lấy từ tổng BE.
- Saved meals có danh sách, tạo/sửa/xóa và quick-add vào ngày/bữa/giờ được chọn;
  giữ snapshot dinh dưỡng. Nếu muốn lưu bữa gồm món từ ảnh/manual, BE nhận snapshot
  có kiểm tra dữ liệu thay vì yêu cầu mọi item đều phải có Food.id.
- Cân nặng dùng record có ngày thực, thêm/sửa/xóa bằng API, biểu đồ sắp theo ngày
  và cập nhật sau ghi. Đồng nhất quy tắc current weight lấy từ record gần nhất
  hợp lệ; không để thêm record quá khứ làm thay cân nặng hiện tại. Giữ quy tắc BE
  không xóa record cuối nếu hồ sơ vẫn cần cân nặng.

### Chat

Màn chat có tin nhắn người dùng/trợ lý, trạng thái đang trả lời, lỗi có thể gửi lại
chủ động, và bubble gợi ý từ response. Gửi ngày đang chọn để BE tạo context phù hợp.
Lưu lịch sử theo user ở BE và nạp lại khi mở; payload bao gồm câu hỏi, câu trả lời,
suggestions và disclaimer. Provider vẫn là Groq hiện có. Không đưa khóa AI vào FE.
Không giả lập câu trả lời thành kết quả AI thật khi provider chưa được cấu hình.

## 6. React Admin

Đặt source tại `admin-web`, dùng React + TypeScript + Vite. Dùng react-admin cho
layout, authProvider, dataProvider, list/form, và thư viện chart cho thống kê.
Giao diện tiếng Việt, đồng bộ phong cách màu hiện có, dùng được trên desktop và
màn nhỏ. Tách các trang users, foods, exercises, analytics và AI history.

- Users: tìm kiếm, phân trang, thông tin tài khoản và đổi trạng thái active/disabled.
  Không tự cho phép cấp role hoặc tạo tài khoản bằng mật khẩu. Không tự khóa tài
  khoản Admin đang thao tác; BE chặn thao tác tương ứng.
- Foods/exercises: bảng, tìm kiếm, phân trang và form thêm/sửa; kiểm tra tên, giá
  trị dinh dưỡng/MET. Delete chuyển is_active=false để giữ log lịch sử; có thể
  kích hoạt lại. Trùng tên trả 409 rõ ràng, không rơi thành lỗi server.
- Analytics: số user, log thực phẩm/vận động/nước và số lần dùng AI; chuỗi dữ liệu
  theo khoảng ngày cho biểu đồ. Phân biệt số user tổng với user hoạt động trong kỳ.
- AI history: lọc theo ngày/user/loại chat hoặc ảnh, phân trang, xem kết quả và
  trạng thái thành công/lỗi. Không lưu ảnh gốc, token hay lỗi thô chứa thông tin
  bí mật. Nội dung tư vấn được lưu để đáp ứng task; chỉ user sở hữu và Admin có
  quyền xem theo endpoint tương ứng.

Endpoint dự kiến: `/admin/users`, `/admin/foods`, `/admin/activities`,
`/admin/analytics`, `/admin/ai-logs`; list trả `{items, total}` và giới hạn phân
trang tại server. DataProvider chuyển sang định dạng react-admin. Có empty,
loading, error và confirmation cho các thao tác đổi trạng thái/xóa.

## 7. Thay đổi BE và một migration mới

Giữ các endpoint đang hoạt động; thêm trường response có mặc định và endpoint mới
để tránh gây hỏng client cũ. Tách router Admin và các phần cấu hình mới khỏi file
tracking đã lớn, nhưng không refactor phần khác ngoài phạm vi tích hợp.

Một revision mới nối tiếp head hợp lệ của bản chính sẽ gom toàn bộ thay đổi schema:

1. Role và ràng buộc role của users, mặc định user cho tài khoản hiện có.
2. WaterSettings: target_ml nullable và serving_ml.
3. NutritionSettings: calo tùy chỉnh, tỷ lệ macro, cân nặng mục tiêu.
4. Thời gian bữa ăn tùy chọn và các trường cần cho snapshot saved meals.
5. Bảng tương tác AI cho lịch sử user và báo cáo Admin, kèm index user/date/type.
6. Cập nhật ràng buộc activity_level nếu thêm mức vận động 1.9.

Không gộp lại toàn bộ lịch sử migration, không thay revision database hiện có,
không tạo lại users/refresh_tokens và không dùng chuỗi `git_*` của BE chia sprint.
Có hướng dẫn backup, kiểm tra current/heads và lệnh upgrade để giáo viên tự chạy.
Nếu current thực tế khác chuỗi source, dừng tại kiểm tra tiền đề và hướng dẫn
đối chiếu; không tự stamp, drop bảng hay sửa dữ liệu để ép upgrade.

## 8. Bảng task, thứ tự và tiêu chí hoàn thành

Foundation trước task: kết nối API, session Google, ánh xạ DTO, lỗi và trạng thái.
Đây là nền chung, không ghi nhận thành task tính năng đã hoàn thành.

| Sprint/task | Số gốc | Phạm vi FE tích hợp | Tiêu chí chính |
|---|---:|---|---|
| S1-T01 | 1 | Form hồ sơ, BMI/TDEE | Lưu/đọc lại BE, preview và kết quả server khớp |
| S1-T02 | 2 | Tiến độ calo và tổng quan ngày | Tổng từ BE đổi sau ghi và theo ngày chọn |
| S1-T03 | 3 | Camera preview và upload | Chụp/chọn → phân tích → sửa → xác nhận ghi |
| S1-T04 | 4 | Search thực phẩm, khẩu phần | Tìm catalog BE, scale theo gram, ghi đúng ngày/bữa |
| S1-T05 | 5 | React Admin login/layout/users | Google login, BE kiểm tra quyền, list/status thật |
| S2-T01 | 6 | Progress dinh dưỡng và biểu đồ macro | Gram/mục tiêu/tỷ lệ 4/4/9 từ dữ liệu thống nhất |
| S2-T02 | 7 | Chọn vận động và thời lượng | Catalog/MET BE và log theo ngày |
| S2-T03 | 8 | Tracker nước và ly | Cộng/trừ/lưu settings, không âm, tải lại không mất |
| S2-T04 | 9 | Saved meals và quick-add | CRUD và thêm vào nhật ký đúng ngày đã chọn |
| S2-T05 | 10 | Form cân nặng và trend chart | Record có ngày, chỉnh/xóa và refresh chart |
| S2-T06 | 11 | React Admin foods/exercises | Bảng/form CRUD thật, giữ snapshot lịch sử |
| S3-T01 | 12 | Chat và suggestion bubbles | Gọi Groq qua BE, giữ lịch sử, xử lý lỗi |
| S3-T02 | 14 | Diary tổng hợp, sửa/xóa | Đúng log ID và ownership, các tổng cập nhật |
| S3-T03 | 15 | Lịch và ngày quá khứ | Marker BE, ngày nhất quán giữa các màn |
| S3-T04 | 16 | React charts và AI logs | Thống kê/lịch sử thật, có lọc và phân trang |

Code bản chính sẽ được phát triển theo các lát chức năng này. Mỗi mốc FE được
chụp tại thời điểm hoàn thành mốc đó, không lấy toàn bộ code cuối gán vào mọi task.
Các phần UI cục bộ vốn có từ baseline được ghi rõ trong tài liệu task, không
nhận là code mới hoặc đã tích hợp sớm ở sprint trước.

## 9. Bộ FE theo sprint và Git

Trong `trien_khai_git_fe` chuẩn bị README, BACKLOG_MAP, dependency map, manifest
checksum, snapshots cộng dồn và patch từ mốc liền trước. Cấu trúc mỗi snapshot
chứa `UI/` với project Android, `admin-web/` khi tới task tương ứng, tài liệu cấu
hình và checklist. Bản cuối khớp source FE bản chính, chỉ loại file local/runtime.

Lịch sử cục bộ có foundation và 15 commit task với nhãn S1/S2/S3; tác giả và ngày
commit là thông tin thực tế khi tạo, không giả lịch sử phát triển hay backdate.
Repo/checkout dành phát hành phải dựa trên lịch sử team remote được kiểm tra lúc
chuẩn bị, dùng branch `codex/frontend-sprints`; chỉ staging đường dẫn `UI/`.
Không commit lại nội dung `BackEnd/`, `FrontEnd/` hoặc thư mục BE chia sprint.
Tất cả công cụ chuẩn bị mặc định không push. Push chỉ thực hiện khi có yêu cầu sau.

Không đưa `.git` của hai repo nguồn, `.env`, virtualenv, SDK path, build output,
crash log hoặc credential vào snapshots/patches/commit. Ghi receipt SHA trước/sau
và checksum để kiểm tra `trien_khai_git_be` được giữ nguyên.

## 10. Kiểm chứng và bàn giao

Sau khi được duyệt kế hoạch thực thi, kiểm tra theo đúng phạm vi thay đổi:

- BE: validation, pure calculations và HTTP integration tests trên DB cô lập;
  kiểm tra ownership, role, user bị disabled, snapshot khi sửa/xóa catalog,
  nhật ký theo ngày, tổng/macro/nước/cân nặng và response Admin.
- Migration: một head hợp lệ, SQL offline và độ khớp model/DDL. Không chạy upgrade
  lên database đang dùng; không gọi Docker hoặc PostgreSQL của giáo viên.
- Google/Groq: dùng mock trong kiểm tra tự động. Đăng nhập Google thực, quota và
  ảnh/chat Groq thực là bước giáo viên kiểm tra với cấu hình riêng.
- Android: build/lint hoặc kiểm tra tương đương theo công cụ thực có; kiểm tra
  navigation camera/chat, account/date guards và refresh. Nếu thiếu Android SDK,
  ghi rõ build chưa chạy, không nhận là đã build thành công.
- React: typecheck/build và kiểm tra các luồng đăng nhập, phân quyền, form/table,
  chart/filter/error trong browser khi môi trường cho phép.
- Artifacts: checksum snapshot/patch, source cuối khớp bản chính, commit đúng
  thứ tự/task và chỉ chứa UI; kiểm tra BE chia sprint không đổi.

Bàn giao hướng dẫn cấu hình cho giáo viên/học sinh, bảng API, bảng task và danh
sách kiểm tra đã chạy/chưa chạy có lý do. Không coi có source hoặc build thành
công là đủ để tuyên bố Google/AI/live migration đã được kiểm chứng.

## 11. Trình tự thực thi đề xuất

Triển khai tuần tự trong chat hiện tại, không dùng sub-agent nếu người dùng không
yêu cầu. Thứ tự: chốt contract và migration → foundation xác thực/kết nối → các
lát S1 → S2 → S3 → kiểm chứng bản cuối và bộ FE theo sprint → bàn giao. Bổ sung
API BE ngay trước lát FE cần dùng để tránh tích hợp dựa trên endpoint chưa tồn tại.

Những phần cố ý ngoài phạm vi là viết lại Android, thêm đăng nhập mật khẩu,
upload tự động dữ liệu cũ, lập thực đơn tuần bằng AI, provider AI khác, production
deployment và nâng cấp DB đang dùng. Không phát sinh thêm tính năng thay cho task
bị bỏ trống số 13.
