str = "hoge";
str = "\n \f \b \r \t  \v \' \" \\ \x46 \117 \uFF4F";
str = new String("foo");
l = str.length;
s = str.charAt(1);
s = str.substring(2);
s = str.substring(1,3);
s = str.slice(2);
s = str.slice(1,3);
s = str.substr(2);
s = str.substr(1,3);

str = "hoge:hogehoge:hogehogehoge";
a = str.split(":");
ss = str.concat(s);
str = "hoge".replace("o","u");
u = a.toUpperCase();
l = u.toLowerCase();

i = "hoge".indexOf("o");
i = "hoge".lastIndexOf("h",2);

str = "hoge".match(/[eg]/);
i = "hoge".search(/og/);