
function f(x: int, y: int) {
	return "(" + x + ", " + y + ")";
}

function f2(x: int, y: int) {
	return "(${x}, ${y})";
}

export function main() {
	assert(f(1,2) == "(1, 2)");
	assert(f2(1,2) == "(1, 2)");
}
