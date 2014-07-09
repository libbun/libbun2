
function CatchZeroDiv(a: int) {
	var n = 0;
	try {
		n = 1 / a;
		assert(false);
	}
	catch(e) {
		n = n + 2;
		println("#Catch(ZeroDivided): " + e)
		println("#Catch(n=2):" + n);
		assert(e instanceof SoftwareFault);
		assert(n == 2);
	}
	assert(n == 2);
}

export function main() {
	CatchZeroDiv(0);
}

