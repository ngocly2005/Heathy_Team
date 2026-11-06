# Kiểm chứng tích hợp

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
