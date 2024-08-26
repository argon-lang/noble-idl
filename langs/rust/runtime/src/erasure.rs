use std::marker::PhantomData;

pub struct ErasedValue {
    data: *mut (),

    clone: fn(*mut ()) -> *mut (),
    release: fn(*mut ()),
}

unsafe impl Send for ErasedValue {}
unsafe impl Sync for ErasedValue {}

impl Clone for ErasedValue {
	fn clone(&self) -> Self {
		let other = (self.clone)(self.data);

		Self {
			data: other,
			clone: self.clone,
			release: self.release,
		}
	}
}

impl Drop for ErasedValue {
	fn drop(&mut self) {
		(self.release)(self.data);
	}
}

pub trait Eraser : Copy {
    type Concrete: Clone + Send + Sync + 'static;

    fn erase(&self, concrete: Self::Concrete) -> ErasedValue;
    unsafe fn unerase(&self, erased: ErasedValue) -> Self::Concrete;
}




fn clone_impl<T: Clone>(t: *mut ()) -> *mut () {
	let t_ref = unsafe { &*(t as *mut T) };
	let t2 = t_ref.clone();
	Box::into_raw(Box::new(t2)) as *mut ()
}

fn release_impl<T>(t: *mut ()) {
	let t = unsafe { Box::from_raw(t as *mut T) };
	drop(t);
}


struct EraserImpl<T>(PhantomData<*const T>);

unsafe impl <T> Send for EraserImpl<T> {}
unsafe impl <T> Sync for EraserImpl<T> {}

impl <T> Clone for EraserImpl<T> {
	fn clone(&self) -> Self {
		Self(PhantomData)
	}
}

impl <T> Copy for EraserImpl<T> {}


impl <T: Clone + Send + Sync + 'static> Eraser for EraserImpl<T> {
    type Concrete = T;

    fn erase(&self, concrete: T) -> ErasedValue {
		let t = Box::new(concrete);
        let ptr = Box::into_raw(t) as *mut ();
        ErasedValue {
			data: ptr,
			clone: clone_impl::<T>,
			release: release_impl::<T>,
		}
    }

    unsafe fn unerase(&self, erased: ErasedValue) -> T {
        let t = Box::from_raw(erased.data as *mut T);
		std::mem::forget(erased);
		*t
    }
}


pub fn make_eraser<T: Clone + Send + Sync + 'static>() -> impl Eraser<Concrete=T> + 'static {
    EraserImpl(PhantomData)
}


#[cfg(test)]
mod tests {
	use std::sync::Arc;
	use super::*;

	trait ExampleMapperInterface<A> {
		fn generic_method<B>(self: Arc<Self>, values: Vec<A>, f: &dyn Fn(A) -> B) -> Vec<B>;
	}


	trait ExampleMapperErased<A> {
		fn generic_method(self: Arc<Self>, values: Vec<A>, f: &dyn Fn(A) -> ErasedValue) -> Vec<ErasedValue>;
	}

	impl <A, S: ExampleMapperInterface<A>> ExampleMapperErased<A> for S {
		fn generic_method(self: Arc<Self>, values: Vec<A>, f: &dyn Fn(A) -> ErasedValue) -> Vec<ErasedValue> {
			<S as ExampleMapperInterface<A>>::generic_method(self, values, f)
		}
	}

	struct ExampleMapper<A> {
		erased: Arc<dyn ExampleMapperErased<A>>,
	}

	impl <A> ExampleMapper<A> {
		fn from<EM: ExampleMapperInterface<A> + 'static>(em: EM) -> Self {
			Self { erased: Arc::new(em) }
		}


		fn generic_method<B: Clone + Send + Sync + 'static>(&self, values: Vec<A>, f: &dyn Fn(A) -> B) -> Vec<B> {
			let eraser = make_eraser();

			let result = self.erased.clone().generic_method(values, &|a| eraser.erase(f(a)));

			result.into_iter().map(|b| unsafe { eraser.unerase(b) }).collect()
		}
	}





	struct ExampleMapperInt;

	impl ExampleMapperInterface<i32> for ExampleMapperInt {
		fn generic_method<B>(self: Arc<Self>, values: Vec<i32>, f: &dyn Fn(i32) -> B) -> Vec<B> {
			values.into_iter().map(f).collect()
		}
	}

	#[test]
	fn mapping_works() {
		let em = ExampleMapper::from(ExampleMapperInt);
		let result = em.generic_method(vec![1, 2, 3], &|i| i.to_string());
		assert_eq!(vec!["1".to_owned(), "2".to_owned(), "3".to_owned()], result);
	}


}





