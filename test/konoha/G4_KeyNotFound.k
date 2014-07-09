
function CatchFault() {
	var n = 0;
	try {
		var m : Map<String> = {}
		var s = m["key"];
		n = 1;
	}
	catch(e) {
		n = n + 2;
		println("#Catch(KeyNotFound): " + e)
		println("#Catch(n=2):" + n);
		assert(e instanceof SoftwareFault);
	}
	assert(n == 2);
}

export function main() {
	CatchFault();
}

