times = getCookie("Times");
if (times == "") {
    times = 1;
}
date = getCookie("Date");
if (date == "") {
    date = "????";
}
document.write("これまでの訪問回数：" + times + "<br>");
document.write("前回の訪問日：" + date + "<br>");

times++;

dd = new Date();
ye = dd.getYear();      if (ye < 1900) ye += 1900;
mo = dd.getMonth() + 1; if (mo < 10) mo = "0" + mo;
da = dd.getDate();      if (da < 10) da = "0" + da;
ho = dd.getHours();     if (ho < 10) ho = "0" + ho;
mi = dd.getMinutes();   if (mi < 10) mi = "0" + mi;
se = dd.getSeconds();   if (se < 10) se = "0" + se;
date = ye + "/" + mo + "/" + da + " " + ho + ":" + mi + ":" + se;

setCookie("Times", times);
setCookie("Date", date);

// clearCookie("Times");
// clearCookie("Date");

function getCookie(key,  tmp1, tmp2, xx1, xx2, xx3) {
    tmp1 = " " + document.cookie + ";";
    xx1 = xx2 = 0;
    len = tmp1.length;
    while (xx1 < len) {
        xx2 = tmp1.indexOf(";", xx1);
        tmp2 = tmp1.substring(xx1 + 1, xx2);
        xx3 = tmp2.indexOf("=");
        if (tmp2.substring(0, xx3) == key) {
            return(unescape(tmp2.substring(xx3 + 1, xx2 - xx1 - 1)));
        }
        xx1 = xx2 + 1;
    }
    return("");
}
function setCookie(key, val, tmp) {
    tmp = key + "=" + escape(val) + "; ";
    // tmp += "path=" + location.pathname + "; ";
    tmp += "expires=Tue, 31-Dec-2030 23:59:59; ";
    document.cookie = tmp;
}
function clearCookie(key) {
    document.cookie = key + "=" + "xx; expires=Tue, 1-Jan-1980 00:00:00;";
}