# PEGO Label Definition

## トップレベル

TopLevelには、特別なラベル #toplevel を付ける

```
#toplevel {
	... // codes here
}
```


## 値・リテラル

### 数値

* 入力例

```
1
1.2
```

* ラベル

```
#integer : 1
#float : 1.2
```

### 文字列

* 入力例

```
"hello"
```

* ラベル

```
#string : hello
```

### シンボル

* 入力例

```
hello
```

* ラベル

```
#name : hello
```

### 配列

* 入力例

```
[1, 2, 3]
```

* ラベル

```
#array {
	#integer : 1
	#integer : 2
	#integer : 3
}
```


### マップ
* 入力例

```
{"a": 1, "b": 2}
```

* ラベル

```
#map {
	#keyvalue {
		#string : a
		#integer : 1
	}
	#keyvalue {
		#string : b
		#integer : 2
	}
}
```

## 演算子

### 四則演算
* 入力例

```
1+2*3-4/2
```

* ラベル

```
#sub {
  #add {
     #integer: 1
     #mul { 
        #integer: 2
        #integer: 3
     }
  }
  #div {
     #integer: 4
     #integer: 2
  }
}
```

### 関係演算
* 入力例

```
1 or 2 and 3
```

* ラベル

```
#or {
   #integer: 1
   #and {
     #integer: 2
     #integer: 3
   }
}
```

### 三項演算子
* 入力例

```
true ? 1 : 2
```

* ラベル

```
#trinary {
	#true : true
	#integer: 1
	#integer: 2
}
```

## 式

### 関数呼び出し

* 入力例

```
hello(a, b, c)
```

* ラベル

```
#apply {
  #name: hello
  #args {
     #name: a
     #name: b
     #name: c
  }
}
```

### 変数宣言、代入
* 入力例

```
let a = 1
var b = 2
a = 3

var c = 4, d = 5
```

* ラベル

```
#let {
	#name: a
	#integer: 1
}

#varlist {
	#var {
		#name: b
		#integer: 2
	}
}

#assign {
	#name: a
	#integer: 3
}

#varlist {
	#var {
		#name: c
		#integer: 4
	}
	#var {
		#name: d
		#integer: 5
	}
}
```

### インデクサ
* 入力例

```
a[3]
```

* ラベル

```
#index {
	#name: a
	#integer: 3
}
```

### フィールド
* 入力例

```
a.b
```

* ラベル

```
#field {
	#name: a
	#name: b
}
```

### グループ
* 入力例

```
(1 + 2) * 3
```

* ラベル

```
#mul {
  #group {
     #add {
        #integer: 1
        #integer: 2
     }
  }
  #integer: 3
}
```


## 文

### if
* 入力例

```
if (5 < 3) {
	4 + 6
}
```

* ラベル

```
#if {
  #lt {
     #integer: 5
     #integer: 3
  }
  #block {
     #add {
        #integer: 4
        #integer: 6
     }
  }
}
```

### for

* 入力例

```
for(i = 0; i < 10; i++ ) {
	a = i
}
```

* ラベル

```
TODO
```


### foreach
* 入力例

```
for( x in [1,2,3]) {
	a = i
}
```

* ラベル

```
#foreach {
	#name: x
	#array {
		#integer: 1
		#integer: 2
		#integer: 3
	}
	#block {
		#assign {
			#name: a
			#name: i
		}
	}
}
```

### while
* 入力例

```
while( a < 0 ) {
	a = i
}
```

* ラベル

```
#while {
	#lt {
		#name: a
		#integer: 3
	}
	#block {
		#assign {
			#name: a
			#name: i
		}
	}
}
```

### 関数定義
TODO: アノテーション
* 入力例

```
function f(n) {
	return n
}

function g(n=1) {
	return n
}

function h(n: int) {
	return n
}
```

* ラベル

```
#function {
	#name: f
	#params {
		#param {
			#name: n
		}
	}
	#block {
		#return {
			#name: n
		}
	}
}
#function {
	#name: g
	#params {
		#param {
			#name: n
			#integer: 1
		}
	}
	#block {
		#return {
			#name: n
		}
	}
}
#function {
	#name: h
	#params {
		#param {
			#name: n
			#type: int
		}
	}
	#block {
		#return {
			#name: n
		}
	}
}
```

### クラス
* 入力例

```
class A extends B {
	function f(n) {
		return n
	}
}
```

* ラベル

```
#class {
	#name: A
	#super {
		#name: B
	}
	#block {
		#function {
			#name: f
			#params {
				#name: n
			}
		#block {
			#return {
				#name: n
			}
		}
	}
}

```

### ブロック
* 入力例

```
{
	1 + 2
}
```

* ラベル

```
#block {
	#add {
		#integer: 1
		#integer: 2
	}
}
```

### モジュール

* 入力例

```
module A {
	1 + 1
}
```

* ラベル

```
#module {
	#add {
		#integer: 1
		#integer: 1
	}
}
```

### Try

* 入力例

```
try {
	1 + 1
} catch(e) {
	b
}
```

* ラベル

```
#try {
	#block {
		#add {
			#integer: 1
			#integer: 1
		}
	}
	#name: e
	#block {
		#name: b
	}
}
```

### インターフェース

TODO

### Enum

TODO

