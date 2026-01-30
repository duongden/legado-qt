# Học Biểu thức chính quy (Regex)

- [Khớp cơ bản]
- [Ký tự meta]
  - [Dấu chấm câu]
  - [Bộ ký tự]
    - [Bộ ký tự phủ định]
  - [Lặp lại]
    - [Dấu sao]
    - [Dấu cộng]
    - [Dấu hỏi]
  - [Dấu ngoặc nhọn]
  - [Nhóm ký tự]
  - [Cấu trúc rẽ nhánh]
  - [Thoát ký tự đặc biệt]
  - [Ký tự định vị]
    - [Dấu mũ]
    - [Dấu đô la]
- [Bộ ký tự viết tắt]
- [Khẳng định (Assert)]
  - [Khẳng định tích cực nhìn trước (Positive Lookahead)]
  - [Khẳng định tiêu cực nhìn trước (Negative Lookahead)]
  - [Khẳng định tích cực nhìn sau (Positive Lookbehind)]
  - [Khẳng định tiêu cực nhìn sau (Negative Lookbehind)]
- [Cờ (Flags)]
  - [Không phân biệt chữ hoa chữ thường]
  - [Tìm kiếm toàn cầu]
  - [Khớp nhiều dòng]
- [Biểu thức chính quy thường dùng]

## 1. Khớp cơ bản

Biểu thức chính quy chỉ là mẫu chúng ta dùng để tìm kiếm chữ cái và số trong văn bản. Ví dụ regex `cat`, nghĩa là: chữ cái `c` theo sau là một chữ cái `a`, tiếp theo là một chữ cái `t`.<pre>"cat" => The <a href="#learn-regex"><strong>cat</strong></a> sat on the mat</pre>

Regex `123` sẽ khớp với chuỗi "123". Việc khớp regex được thực hiện bằng cách so sánh từng ký tự trong regex với từng ký tự trong chuỗi cần khớp.
Regex thường phân biệt chữ hoa chữ thường, vì vậy regex `Cat` không khớp với chuỗi "cat".<pre>"Cat" => The cat sat on the <a href="#learn-regex"><strong>Cat</strong></a></pre>

## 2. Ký tự meta

Ký tự meta là thành phần cơ bản của biểu thức chính quy. Ở đây ký tự meta không mang ý nghĩa thông thường của nó, mà được giải thích theo một ý nghĩa đặc biệt nào đó. Một số ký tự meta khi viết trong ngoặc vuông sẽ có ý nghĩa đặc biệt.
Các ký tự meta như sau:

|Ký tự meta|Mô tả|
|:----:|----|
|.|Khớp với bất kỳ ký tự nào ngoại trừ ký tự xuống dòng.|
|[ ]|Lớp ký tự, khớp với bất kỳ ký tự nào nằm trong dấu ngoặc vuông.|
|[^ ]|Lớp ký tự phủ định. Khớp với bất kỳ ký tự nào không nằm trong dấu ngoặc vuông|
|*|Khớp với biểu thức con phía trước không hoặc nhiều lần|
|+|Khớp với biểu thức con phía trước một hoặc nhiều lần|
|?|Khớp với biểu thức con phía trước không hoặc một lần, hoặc chỉ định một định lượng không tham lam.|
|{n,m}|Dấu ngoặc nhọn, khớp với ký tự phía trước ít nhất n lần, nhưng không quá m lần.|
|(xyz)|Nhóm ký tự, khớp với các ký tự xyz theo đúng thứ tự.|
|&#124;|Cấu trúc rẽ nhánh, khớp với ký tự trước hoặc sau ký hiệu.|
|&#92;|Ký tự thoát, nó có thể khôi phục ý nghĩa ban đầu của ký tự meta, cho phép bạn khớp các ký tự dành riêng <code>[ ] ( ) { } . * + ? ^ $ \ &#124;</code>|
|^|Khớp bắt đầu dòng|
|$|Khớp kết thúc dòng|

## 2.1 Dấu chấm câu

Dấu chấm câu `.` là ví dụ đơn giản nhất về ký tự meta. Ký tự meta `.` có thể khớp với bất kỳ ký tự đơn nào. Nó sẽ không khớp với ký tự xuống dòng và ký tự dòng mới. Ví dụ regex `.ar`, nghĩa là: bất kỳ ký tự nào theo sau là một chữ cái `a`,
tiếp theo là một chữ cái `r`.<pre>".ar" => The <a href="#learn-regex"><strong>car</strong></a> <a href="#learn-regex"><strong>par</strong></a>ked in the <a href="#learn-regex"><strong>gar</strong></a>age.</pre>

## 2.2 Bộ ký tự

Bộ ký tự cũng được gọi là lớp ký tự. Dấu ngoặc vuông được sử dụng để chỉ định bộ ký tự. Sử dụng dấu gạch ngang trong bộ ký tự để chỉ định phạm vi ký tự. Thứ tự của phạm vi ký tự trong dấu ngoặc vuông không quan trọng.
Ví dụ regex `[Tt]he`, nghĩa là: chữ `T` hoa hoặc `t` thường, theo sau là chữ cái `h`, tiếp theo là chữ cái `e`.<pre>"[Tt]he" => <a href="#learn-regex"><strong>The</strong></a> car parked in <a href="#learn-regex"><strong>the</strong></a> garage.</pre>

Tuy nhiên, dấu chấm câu trong bộ ký tự biểu thị ý nghĩa theo nghĩa đen của nó. Regex `ar[.]`, nghĩa là chữ cái thường `a`, theo sau là một chữ cái `r`, tiếp theo là một ký tự dấu chấm câu `.`.<pre>"ar[.]" => A garage is a good place to park a c<a href="#learn-regex"><strong>ar.</strong></a></pre>

### 2.2.1 Bộ ký tự phủ định

Nói chung chèn ký tự `^` biểu thị sự bắt đầu của một chuỗi, nhưng khi nó xuất hiện trong dấu ngoặc vuông, nó sẽ phủ định bộ ký tự. Ví dụ regex `[^c]ar`, nghĩa là: bất kỳ ký tự nào ngoại trừ chữ cái `c`, theo sau là ký tự `a`,
tiếp theo là một chữ cái `r`.<pre>"[^c]ar" => The car <a href="#learn-regex"><strong>par</strong></a>ked in the <a href="#learn-regex"><strong>gar</strong></a>age.</pre>

## 2.3 Lặp lại

Các ký tự meta `+`, `*` hoặc `?` sau đây được sử dụng để chỉ định mẫu con có thể xuất hiện bao nhiêu lần. Tác dụng của các ký tự meta này khác nhau trong các tình huống khác nhau.

### 2.3.1 Dấu sao

Ký hiệu `*` biểu thị khớp với quy tắc khớp trước đó không hoặc nhiều lần. Regex `a*` biểu thị chữ cái thường `a` có thể lặp lại không hoặc nhiều lần. Nhưng nếu nó xuất hiện sau bộ ký tự hoặc lớp ký tự, nó biểu thị sự lặp lại của toàn bộ bộ ký tự.
Ví dụ regex `[a-z]*`, nghĩa là: một dòng có thể chứa bất kỳ số lượng chữ cái thường nào.<pre>"[a-z]*" => T<a href="#learn-regex"><strong>he</strong></a> <a href="#learn-regex"><strong>car</strong></a> <a href="#learn-regex"><strong>parked</strong></a> <a href="#learn-regex"><strong>in</strong></a> <a href="#learn-regex"><strong>the</strong></a> <a href="#learn-regex"><strong>garage</strong></a> #21.</pre>

Ký hiệu `*` này có thể được sử dụng với ký hiệu meta `.` để khớp với bất kỳ chuỗi nào `.*`. Ký hiệu `*` này có thể được sử dụng với ký tự khoảng trắng `\s` để khớp với một chuỗi ký tự khoảng trắng.
Ví dụ regex `\s*cat\s*`, nghĩa là: không hoặc nhiều khoảng trắng, theo sau là chữ cái thường `c`, tiếp theo là chữ cái thường `a`, tiếp theo là chữ cái thường `t`, theo sau là không hoặc nhiều khoảng trắng.<pre>"\s*cat\s*" => The fat<a href="#learn-regex"><strong> cat </strong></a>sat on the <a href="#learn-regex"><strong>cat</strong></a>.</pre>

### 2.3.2 Dấu cộng

Ký hiệu `+` khớp với ký tự trước đó một hoặc nhiều lần. Ví dụ regex `c.+t`, nghĩa là: một chữ cái thường `c`, theo sau là bất kỳ số lượng ký tự nào, theo sau là chữ cái thường `t`.<pre>"c.+t" => The fat <a href="#learn-regex"><strong>cat sat on the mat</strong></a>.</pre>

### 2.3.3 Dấu hỏi

Trong biểu thức chính quy, ký tự meta `?` được sử dụng để biểu thị ký tự trước đó là tùy chọn. Ký hiệu này khớp với ký tự trước đó không hoặc một lần.
Ví dụ regex `[T]?he`, nghĩa là: chữ cái hoa `T` tùy chọn, theo sau là chữ cái thường `h`, theo sau là chữ cái thường `e`.<pre>"[T]he" => <a href="#learn-regex"><strong>The</strong></a> car is parked in the garage.</pre><pre>"[T]?he" => <a href="#learn-regex"><strong>The</strong></a> car is parked in t<a href="#learn-regex"><strong>he</strong></a> garage.</pre>

## 2.4 Dấu ngoặc nhọn

Trong biểu thức chính quy, dấu ngoặc nhọn (còn được gọi là lượng từ ?) được sử dụng để chỉ định số lần ký tự hoặc nhóm ký tự có thể lặp lại. Ví dụ regex `[0-9]{2,3}`, nghĩa là: khớp ít nhất 2 chữ số nhưng không quá 3 chữ số (ký tự trong phạm vi 0 đến 9).<pre>"[0-9]{2,3}" => The number was 9.<a href="#learn-regex"><strong>999</strong></a>7 but we rounded it off to <a href="#learn-regex"><strong>10</strong></a>.0.</pre>

Chúng ta có thể bỏ qua số thứ hai. Ví dụ regex `[0-9]{2,}`, nghĩa là: khớp 2 hoặc nhiều chữ số. Nếu chúng ta cũng xóa dấu phẩy, thì regex `[0-9]{2}`, nghĩa là: khớp chính xác 2 chữ số.<pre>"[0-9]{2,}" => The number was 9.<a href="#learn-regex"><strong>9997</strong></a> but we rounded it off to <a href="#learn-regex"><strong>10</strong></a>.0.</pre><pre>"[0-9]{2}" => The number was 9.<a href="#learn-regex"><strong>99</strong></a><a href="#learn-regex"><strong>97</strong></a> but we rounded it off to <a href="#learn-regex"><strong>10</strong></a>.0.</pre>

## 2.5 Nhóm ký tự

Nhóm ký tự là một nhóm mẫu con được viết trong dấu ngoặc tròn `(...)`. Như chúng ta đã thảo luận trong biểu thức chính quy, nếu chúng ta đặt một lượng từ sau một ký tự, nó sẽ lặp lại ký tự trước đó.
Tuy nhiên, nếu chúng ta đặt lượng từ sau một nhóm ký tự, nó sẽ lặp lại toàn bộ nhóm ký tự.
Ví dụ regex `(ab)*` biểu thị khớp không hoặc nhiều chuỗi "ab". Chúng ta cũng có thể sử dụng ký tự meta `|` trong nhóm ký tự. Ví dụ regex `(c|g|p)ar`, nghĩa là: chữ cái thường `c`, `g` hoặc `p` theo sau là chữ cái `a`, theo sau là chữ cái `r`.<pre>"(c|g|p)ar" => The <a href="#learn-regex"><strong>car</strong></a> is <a href="#learn-regex"><strong>par</strong></a>ked in the <a href="#learn-regex"><strong>gar</strong></a>age.</pre>

## 2.6 Cấu trúc rẽ nhánh

Trong biểu thức chính quy, thanh dọc `|` được sử dụng để định nghĩa cấu trúc rẽ nhánh, cấu trúc rẽ nhánh giống như điều kiện giữa nhiều biểu thức. Bây giờ bạn có thể nghĩ rằng bộ ký tự và cấu trúc rẽ nhánh hoạt động giống nhau.
Nhưng sự khác biệt lớn giữa bộ ký tự và cấu trúc rẽ nhánh là bộ ký tự chỉ hoạt động ở cấp độ ký tự, trong khi cấu trúc rẽ nhánh vẫn có thể hoạt động ở cấp độ biểu thức.
Ví dụ regex `(T|t)he|car`, nghĩa là: chữ cái hoa `T` hoặc chữ cái thường `t`, theo sau là chữ cái thường `h`, theo sau là chữ cái thường `e` hoặc chữ cái thường `c`, theo sau là chữ cái thường `a`, theo sau là chữ cái thường `r`.<pre>"(T|t)he|car" => <a href="#learn-regex"><strong>The</strong></a> <a href="#learn-regex"><strong>car</strong></a> is parked in <a href="#learn-regex"><strong>the</strong></a> garage.</pre>

## 2.7 Thoát ký tự đặc biệt

Dấu gạch chéo ngược `\` được sử dụng trong biểu thức chính quy để thoát ký tự tiếp theo. Điều này sẽ cho phép bạn sử dụng các ký tự dành riêng làm ký tự khớp `{ } [ ] / \ + * . $ ^ | ?`. Thêm `\` vào trước ký tự đặc biệt để sử dụng nó làm ký tự khớp.
Ví dụ regex `.` được sử dụng để khớp với bất kỳ ký tự nào ngoại trừ ký tự xuống dòng. Bây giờ để khớp ký tự `.` trong chuỗi đầu vào, regex `(f|c|m)at\.?`, nghĩa là: chữ cái thường `f`, `c` hoặc `m` theo sau là chữ cái thường `a`, theo sau là chữ cái thường `t`, theo sau là ký tự `.` tùy chọn.<pre>"(f|c|m)at\.?" => The <a href="#learn-regex"><strong>fat</strong></a> <a href="#learn-regex"><strong>cat</strong></a> sat on the <a href="#learn-regex"><strong>mat.</strong></a></pre>

## 2.8 Ký tự định vị

Trong biểu thức chính quy, để kiểm tra xem ký hiệu khớp có phải là ký hiệu bắt đầu hay kết thúc hay không, chúng ta sử dụng ký tự định vị.
Có hai loại ký tự định vị: loại thứ nhất là `^` kiểm tra xem ký tự khớp có phải là ký tự bắt đầu hay không, loại thứ hai là `$`, nó kiểm tra xem ký tự khớp có phải là ký tự cuối cùng của chuỗi đầu vào hay không.

### 2.8.1 Dấu mũ

Ký hiệu dấu mũ `^` được sử dụng để kiểm tra xem ký tự khớp có phải là ký tự đầu tiên của chuỗi đầu vào hay không. Nếu chúng ta sử dụng regex `^a` (nếu a là ký hiệu bắt đầu) để khớp chuỗi `abc`, nó sẽ khớp với `a`.
Nhưng nếu chúng ta sử dụng regex `^b`, nó sẽ không khớp với bất cứ thứ gì, vì trong chuỗi `abc`, "b" không phải là ký tự bắt đầu.
Hãy xem một regex khác `^(T|t)he`, điều này có nghĩa là: chữ cái hoa `T` hoặc chữ cái thường `t` là ký hiệu bắt đầu của chuỗi đầu vào, theo sau là chữ cái thường `h`, theo sau là chữ cái thường `e`.<pre>"(T|t)he" => <a href="#learn-regex"><strong>The</strong></a> car is parked in <a href="#learn-regex"><strong>the</strong></a> garage.</pre><pre>"^(T|t)he" => <a href="#learn-regex"><strong>The</strong></a> car is parked in the garage.</pre>

### 2.8.2 Dấu đô la

Ký hiệu đô la `$` được sử dụng để kiểm tra xem ký tự khớp có phải là ký tự cuối cùng của chuỗi đầu vào hay không. Ví dụ regex `(at\.)$`, nghĩa là: chữ cái thường `a`, theo sau là chữ cái thường `t`, theo sau là một ký tự `.`, và bộ khớp này phải là phần cuối của chuỗi.<pre>"(at\.)" => The fat c<a href="#learn-regex"><strong>at.</strong></a> s<a href="#learn-regex"><strong>at.</strong></a> on the m<a href="#learn-regex"><strong>at.</strong></a></pre><pre>"(at\.)$" => The fat cat sat on the m<a href="#learn-regex"><strong>at.</strong></a></pre>

## 3. Bộ ký tự viết tắt

Biểu thức chính quy cung cấp các từ viết tắt cho các bộ ký tự thường dùng và các biểu thức chính quy thường dùng. Các bộ ký tự viết tắt như sau:

|Viết tắt|Mô tả|
|:----:|----|
|.|Khớp với bất kỳ ký tự nào ngoại trừ ký tự xuống dòng|
|\w|Khớp với tất cả các ký tự chữ cái và số: `[a-zA-Z0-9_]`|
|\W|Khớp với các ký tự không phải chữ cái và số: `[^\w]`|
|\d|Khớp với số: `[0-9]`|
|\D|Khớp với không phải số: `[^\d]`|
|\s|Khớp với ký tự khoảng trắng: `[\t\n\f\r\p{Z}]`|
|\S|Khớp với ký tự không phải khoảng trắng: `[^\s]`|

## 4. Khẳng định (Assert)

Khẳng định nhìn sau và khẳng định nhìn trước đôi khi được gọi là khẳng định, chúng là loại ***nhóm không bắt giữ*** đặc biệt (được sử dụng để khớp mẫu, nhưng không bao gồm trong danh sách khớp). Khi chúng ta có mẫu này trước hoặc sau một mẫu cụ thể, khẳng định sẽ được sử dụng ưu tiên.
Ví dụ: chúng ta muốn lấy tất cả các số có tiền tố `$` trong chuỗi đầu vào `$4.44 and $10.88`. Chúng ta có thể sử dụng regex này `(?<=\$)[0-9\.]*`, nghĩa là: lấy tất cả các số có chứa ký tự `.` và có tiền tố `$`.
Sau đây là các khẳng định được sử dụng trong biểu thức chính quy:

|Ký hiệu|Mô tả|
|:----:|----|
|?=|Khẳng định tích cực nhìn trước (Positive Lookahead)|
|?!|Khẳng định tiêu cực nhìn trước (Negative Lookahead)|
|?<=|Khẳng định tích cực nhìn sau (Positive Lookbehind)|
|?<!|Khẳng định tiêu cực nhìn sau (Negative Lookbehind)|

### 4.1 Khẳng định tích cực nhìn trước

Khẳng định tích cực nhìn trước cho rằng phần đầu tiên của biểu thức phải là biểu thức khẳng định nhìn trước. Kết quả khớp trả về chỉ chứa văn bản khớp với phần đầu tiên của biểu thức.
Để định nghĩa một khẳng định tích cực nhìn trước trong ngoặc đơn, dấu hỏi và dấu bằng được sử dụng như sau `(?=...)`. Biểu thức khẳng định nhìn trước được viết sau dấu bằng trong ngoặc đơn.
Ví dụ regex `(T|t)he(?=\sfat)`, nghĩa là: khớp chữ cái hoa `T` hoặc chữ cái thường `t`, theo sau là chữ cái `h`, theo sau là chữ cái `e`.
Trong ngoặc đơn, chúng ta đã định nghĩa khẳng định tích cực nhìn trước, nó sẽ hướng dẫn công cụ regex khớp `The` hoặc `the` theo sau là `fat`.<pre>"(T|t)he(?=\sfat)" => <a href="#learn-regex"><strong>The</strong></a> fat cat sat on the mat.</pre>

### 4.2 Khẳng định tiêu cực nhìn trước

Khi chúng ta cần lấy nội dung không khớp với biểu thức từ chuỗi đầu vào, hãy sử dụng khẳng định tiêu cực nhìn trước. Định nghĩa của khẳng định tiêu cực nhìn trước giống như chúng ta định nghĩa khẳng định tích cực nhìn trước,
sự khác biệt duy nhất không phải là dấu bằng `=`, chúng ta sử dụng ký hiệu phủ định `!`, ví dụ `(?!...)`.
Hãy xem regex sau `(T|t)he(?!\sfat)`, nghĩa là: lấy tất cả `The` hoặc `the` từ chuỗi đầu vào mà không khớp với `fat` có thêm một ký tự khoảng trắng ở trước.<pre>"(T|t)he(?!\sfat)" => The fat cat sat on <a href="#learn-regex"><strong>the</strong></a> mat.</pre>

### 4.3 Khẳng định tích cực nhìn sau

Khẳng định tích cực nhìn sau được sử dụng để lấy tất cả nội dung khớp trước một mẫu cụ thể. Khẳng định tích cực nhìn sau được biểu thị là `(?<=...)`. Ví dụ regex `(?<=(T|t)he\s)(fat|mat)`, nghĩa là: lấy tất cả các từ `fat` và `mat` sau từ `The` hoặc `the` từ chuỗi đầu vào.<pre>"(?<=(T|t)he\s)(fat|mat)" => The <a href="#learn-regex"><strong>fat</strong></a> cat sat on the <a href="#learn-regex"><strong>mat</strong></a>.</pre>

### 4.4 Khẳng định tiêu cực nhìn sau

Khẳng định tiêu cực nhìn sau được sử dụng để lấy tất cả nội dung khớp không nằm trước một mẫu cụ thể. Khẳng định tiêu cực nhìn sau được biểu thị là `(?<!...)`. Ví dụ regex `(?<!(T|t)he\s)(cat)`, nghĩa là: lấy tất cả các từ `cat` không nằm sau `The` hoặc `the` trong ký tự đầu vào.<pre>"(?&lt;!(T|t)he\s)(cat)" => The cat sat on <a href="#learn-regex"><strong>cat</strong></a>.</pre>

## 5. Cờ

Cờ còn được gọi là công cụ sửa đổi, vì nó sẽ sửa đổi đầu ra của biểu thức chính quy. Các cờ này có thể được sử dụng theo bất kỳ thứ tự hoặc kết hợp nào và là một phần của biểu thức chính quy.

|Cờ|Mô tả|
|:----:|----|
|i|Không phân biệt chữ hoa chữ thường: Đặt khớp thành không phân biệt chữ hoa chữ thường.|
|g|Tìm kiếm toàn cầu: Tìm kiếm tất cả các khớp trong toàn bộ chuỗi đầu vào.|
|m|Khớp nhiều dòng: Sẽ khớp từng dòng của chuỗi đầu vào.|

* **Số**: `\d+$`
* **Tên người dùng**: `^[\w\d_.]{4,16}$`
* **Ký tự chữ cái và số**: `^[a-zA-Z0-9]*$`
* **Ký tự chữ cái và số có khoảng trắng**: `^[a-zA-Z0-9 ]*$`
* **Chữ cái thường**: `[a-z]+$`
* **Chữ cái hoa**: `[A-Z]+$`
* **URL**: `^(((http|https|ftp):\/\/)?([[a-zA-Z0-9]\-\.])+(\.)([[a-zA-Z0-9]]){2,4}([[a-zA-Z0-9]\/+=%&_\.~?\-]*))*$`
* **Ngày (MM/DD/YYYY)**: `^(0?[1-9]|1[012])[- /.](0?[1-9]|[12][0-9]|3[01])[- /.](19|20)?[0-9]{2}$`
* **Ngày (YYYY/MM/DD)**: `^(19|20)?[0-9]{2}[- /.](0?[1-9]|1[012])[- /.](0?[1-9]|[12][0-9]|3[01])$`
* **Xin cập nhật, xin chuyển tiếp, cảm ơn**: `[\(（【].*?[求更谢乐发推].*?[】）\)]`
* **Tìm chương mới nhất**: `Bạn có thể.*?tìm chương mới nhất`
* **ps/PS**: `(?i)ps\b.*`
* **Thẻ Html**: `<[^>]+?>`
