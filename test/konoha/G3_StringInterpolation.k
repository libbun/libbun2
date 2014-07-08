

function f(x: int, y :int, z: int) {
	println("#(${x})");
	println("#(${x},${y},${z})");
	println("#${x}${y}${z}");
	assert("(${x})" == "(0)");
	assert("(${x},${y},${z})" == "(0,1,2)");
	assert("${x}${y}${z}" == "012");
}

export function main() {
	f(0, 1, 2);
}
	
