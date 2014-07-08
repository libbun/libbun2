
function CatchFault() {
	var n = 0;
	try {
		var a : int[] = []
		var s = a[9];
		n = 1;
	}
	catch(e) {
		n = n + 2;
		println("#Catch(OutOfIndex): " + e)
		println("#Catch(n=2):" + n);
		assert(e instanceof SoftwareFault);
	}
	assert(n == 2);
}

export function main() {
	CatchFault();
}

