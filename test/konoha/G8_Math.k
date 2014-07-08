require math;

export function main() {
	println("#pi: " + math::PI);
	println("#sin(0.1): " + math::sin(0.1));
	assert(math::PI > 3.1415);
	assert(math::PI < 3.1416);
}

