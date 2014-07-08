
function f() {
	return asm("1"):int;
}

export function main() {
	assert(f() == 1);
}
