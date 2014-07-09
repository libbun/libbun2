function hoge(a) {
    a++;
}

function hogehoge(num,str){
    num++;
    document.write(str);
}

function foo(){
    document.write("Foo!!");
}

  foo();
  hogehoge(2,"hoge");
  hoge(3);