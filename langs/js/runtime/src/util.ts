

export type PromiseWithErrror<A, _E extends Error> = Promise<A>;


export interface ErrorChecker<E extends Error> {
	isInstance(x: Error): x is E;
}

export namespace ErrorChecker {
	export function fromConstructor<E extends Error>(ctor: new(...args: unknown[]) => E): ErrorChecker<E> {
		return new ErrorCheckerImpl<E>(ctor);
	}
}

class ErrorCheckerImpl<E extends Error> implements ErrorChecker<E> {
	constructor(ctor: new(...args: unknown[]) => E) {
		this.#ctor = ctor;
	}

	readonly #ctor: new(...args: unknown[]) => E;


	isInstance(x: Error): x is E {
		return x instanceof this.#ctor;
	}
}
