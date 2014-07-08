

export function main() {
	try {
		x++;          // this must be an error
		assert(false);
	}
	catch(e) {
		println("#SoftwareFault: "+ e);
		assert(e instanceof SoftwareFault);
	}
}

