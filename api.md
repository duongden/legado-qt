# [API](/app/src/main/java/io/legado/app/api/controller) Legado

## Cấu hình cho [Web](/app/src/main/java/io/legado/app/web/)

Bạn cần bật "Web Service" (Máy chủ Web) trong phần cài đặt trước.

## Cách sử dụng

### Web

Các hướng dẫn dưới đây giả sử bạn thao tác trên máy nội bộ và cổng mở là 1234.  
Nếu bạn muốn truy cập [Legado]() từ thiết bị từ xa (ví dụ PC), hãy thay `127.0.0.1` bằng địa chỉ IP của điện thoại.

#### Thêm một Nguồn truyện (Book Source)

Nội dung BODY request là chuỗi `JSON`,  
Định dạng tham khảo [file này](/app/src/main/java/io/legado/app/data/entities/BookSource.kt)

```
URL = http://127.0.0.1:1234/saveBookSource
Method = POST
```

#### Thêm nhiều Nguồn truyện hoặc Nguồn RSS

Nội dung BODY request là chuỗi `JSON`,  
Định dạng tham khảo [file này](/app/src/main/java/io/legado/app/data/entities/BookSource.kt), **là định dạng Mảng (Array)**.

```
URL = http://127.0.0.1:1234/saveBookSources
URL = http://127.0.0.1:1234/saveRssSources
Method = POST
```

#### Lấy thông tin Nguồn

```
URL = http://127.0.0.1:1234/getBookSource?url=xxx
URL = http://127.0.0.1:1234/getRssSource?url=xxx
Method = GET
``` 

#### Lấy tất cả Nguồn truyện hoặc Nguồn RSS

```
URL = http://127.0.0.1:1234/getBookSources
URL = http://127.0.0.1:1234/getRssSources
Method = GET
```

#### Xóa nhiều Nguồn truyện hoặc Nguồn RSS

Nội dung BODY request là chuỗi `JSON`,  
Định dạng tham khảo [file này](/app/src/main/java/io/legado/app/data/entities/BookSource.kt), **là định dạng Mảng (Array)**.

```
URL = http://127.0.0.1:1234/deleteBookSources
URL = http://127.0.0.1:1234/deleteRssSources
Method = POST
```

#### Debug nguồn

Key là từ khóa tìm kiếm, tag là link nguồn

```
URL = ws://127.0.0.1:1235/bookSourceDebug
URL = ws://127.0.0.1:1235/rssSourceDebug
Message = { key: [String], tag: [String] }
```

#### Lấy quy tắc thay thế

```
URL = http://127.0.0.1:1234/getReplaceRules
Method = GET
```

#### Quản lý quy tắc thay thế

Nội dung BODY request là chuỗi `JSON`,  
Quy tắc thay thế tham khảo [file này](/app/src/main/java/io/legado/app/data/entities/ReplaceRule.kt).

##### Xóa

```
URL = http://127.0.0.1:1234/deleteReplaceRule
Method = POST
Body = [ReplaceRule]
```
##### Thêm

```
URL = http://127.0.0.1:1234/saveReplaceRule
Method = POST
Body = [ReplaceRule]
```

##### Test

Trả về kết quả thay thế văn bản text

```
URL = http://127.0.0.1:1234/testReplaceRule
Method = POST
Body = { rule: [ReplaceRule], text: [String] }
```

#### Tìm kiếm sách online

Nếu muốn lấy mục lục và nội dung sách tương ứng, vui lòng **Thêm sách** trước để bật bộ nhớ đệm, nếu đọc thử xong quyết định không thêm vào kệ sách thì hãy **Xóa sách**

```
URL = ws://127.0.0.1:1235/searchBook
Message = { key: [String] }
```

#### Thêm sách

Nội dung BODY request là chuỗi `JSON`,  
Định dạng tham khảo [file này](/app/src/main/java/io/legado/app/data/entities/Book.kt).

```
URL = http://127.0.0.1:1234/saveBook
Method = POST
```

#### Xóa sách

```
URL = http://127.0.0.1:1234/deleteBook
Method = POST
```

#### Lấy tất cả sách

```
URL = http://127.0.0.1:1234/getBookshelf
Method = GET
```

Lấy tất cả sách trong App.

#### Lấy danh sách chương của sách

```
URL = http://127.0.0.1:1234/getChapterList?url=xxx
Method = GET
```

Lấy danh sách chương của sách chỉ định.

#### Lấy nội dung sách

```
URL = http://127.0.0.1:1234/getBookContent?url=xxx&index=1
Method = GET
```

Lấy nội dung văn bản của chương thứ `index` của sách chỉ định.

#### Lấy ảnh bìa

```
URL = http://127.0.0.1:1234/cover?path=xxxxx
Method = GET
```

#### Lấy hình ảnh trong nội dung

```
URL = http://127.0.0.1:1234/image?url=${bookUrl}&path=${picUrl}&width=${width}
Method = GET
```

#### Lưu tiến độ đọc

Nội dung BODY request là chuỗi `JSON`,  
Định dạng tham khảo [file này](/app/src/main/java/io/legado/app/data/entities/BookProgress.kt).

```
URL = http://127.0.0.1:1234/saveBookProgress
Method = POST
```

### [Content Provider](/app/src/main/java/io/legado/app/api/ReaderProvider.kt)


* Cần khai báo quyền `io.legado.READ_WRITE`
* `providerHost` là `Tên_bao.readerProvider`, ví dụ `io.legado.app.release.readerProvider`, địa chỉ khác nhau cho các gói khác nhau để tránh xung đột
* Vui lòng tự thay thế `providerHost` xuất hiện bên dưới

#### Thêm một Nguồn truyện hoặc Nguồn RSS

Tạo `ContentValues` với `Key="json"`, nội dung là chuỗi `JSON`,  
Định dạng tham khảo [file này](/app/src/main/java/io/legado/app/data/entities/BookSource.kt)

```
URL = content://providerHost/bookSource/insert
URL = content://providerHost/rssSource/insert
Method = insert
```

#### Thêm nhiều Nguồn truyện hoặc Nguồn RSS

Tạo `ContentValues` với `Key="json"`, nội dung là chuỗi `JSON`,  
Định dạng tham khảo [file này](/app/src/main/java/io/legado/app/data/entities/BookSource.kt), **là định dạng Mảng (Array)**.

```
URL = content://providerHost/bookSources/insert
URL = content://providerHost/rssSources/insert
Method = insert
```

#### Lấy thông tin Nguồn truyện hoặc Nguồn RSS

Lấy thông tin nguồn tương ứng với URL chỉ định.  
Dùng `Cursor.getString(0)` để lấy kết quả trả về.

```
URL = content://providerHost/bookSource/query?url=xxx
URL = content://providerHost/rssSource/query?url=xxx
Method = query
```

#### Lấy tất cả Nguồn truyện hoặc Nguồn RSS

Lấy tất cả nguồn trong App.  
Dùng `Cursor.getString(0)` để lấy kết quả trả về.

```
URL = content://providerHost/bookSources/query
URL = content://providerHost/rssSources/query
Method = query
```

#### Xóa nhiều Nguồn truyện hoặc Nguồn RSS

Tạo `ContentValues` với `Key="json"`, nội dung là chuỗi `JSON`,  
Định dạng tham khảo [file này](/app/src/main/java/io/legado/app/data/entities/BookSource.kt), **là định dạng Mảng (Array)**.

```
URL = content://providerHost/bookSources/delete
URL = content://providerHost/rssSources/delete
Method = delete
```

#### Thêm sách

Tạo `ContentValues` với `Key="json"`, nội dung là chuỗi `JSON`,  
Định dạng tham khảo [file này](/app/src/main/java/io/legado/app/data/entities/Book.kt).

```
URL = content://providerHost/book/insert
Method = insert
```

#### Lấy tất cả sách

Lấy tất cả sách trong App.  
Dùng `Cursor.getString(0)` để lấy kết quả trả về.

```
URL = content://providerHost/books/query
Method = query
```

#### Lấy danh sách chương của sách

Lấy danh sách chương của sách chỉ định.   
Dùng `Cursor.getString(0)` để lấy kết quả trả về.

```
URL = content://providerHost/book/chapter/query?url=xxx
Method = query
```

#### Lấy nội dung sách

Lấy nội dung văn bản của chương thứ `index` của sách chỉ định.     
Dùng `Cursor.getString(0)` để lấy kết quả trả về.

```
URL = content://providerHost/book/content/query?url=xxx&index=1
Method = query
```

#### Lấy ảnh bìa

```
URL = content://providerHost/book/cover/query?path=xxxx
Method = query
```
