

export type PromiseWithErrror<A, _E> = Promise<A>;


export type NobleIdlError<Name extends string> = Error & {
	readonly [ErrorChecker.nobleidlErrorTypeSymbol]: Name;
	readonly information: unknown;
}


export interface ErrorChecker<E> {
	isInstance(x: unknown): x is E;
}

export namespace ErrorChecker {
	export const nobleidlErrorTypeSymbol: unique symbol = Symbol.for("nobleidl-error-type");

	export function fromTypeName<Name extends string, E extends NobleIdlError<Name>>(name: Name): ErrorChecker<E> {
		return new ErrorCheckerImpl<Name, E>(name);
	}
}

class ErrorCheckerImpl<Name extends string, E extends NobleIdlError<Name>> implements ErrorChecker<E> {
	constructor(name: Name) {
		this.#errorType = name;
	}

	readonly #errorType: string;


	isInstance(x: Error): x is E {
		return ErrorChecker.nobleidlErrorTypeSymbol in x && x.name === this.#errorType;
	}
}
