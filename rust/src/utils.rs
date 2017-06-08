use jni::JNIEnv;
use jni::sys::{jlong, jbyteArray};

use std::panic;
use std::any::Any;
use std::thread::Result;
use std::error::Error;

// Panics if object is equal to zero.
pub fn cast_object<T>(object: jlong) -> &'static mut T {
    assert!(object != 0);
    let ptr = object as *mut T;
    unsafe { &mut *ptr }
}

pub fn bytes_array_to_vec(_env: &JNIEnv, _array: jbyteArray) -> Vec<u8> {
    // TODO: FIXME.
    unimplemented!();
}

// Constructs `Box` from raw pointer and immediately drops it.
pub fn drop_object<T>(env: &JNIEnv, object: jlong) {
    let res = panic::catch_unwind(|| unsafe {
                                      Box::from_raw(object as *mut T);
                                  });
    // TODO: Should we throw exception here or just log error?
    unwrap_or_exception(env, res);
}

// Returns value or "throws" exception. Default value is returned, because exception will be thrown
// at the Java side. So this function should be used only for the `panic::catch_unwind` result.
pub fn unwrap_or_exception<T: Default>(env: &JNIEnv, res: Result<T>) -> T {
    match res {
        Err(ref e) => {
            throw(env, &any_to_string(e));
        }
        _ => {}
    }

    res.unwrap_or_default()
}

// Calls a corresponding `JNIEnv` method, so exception will be thrown when execution returns to
// the Java side.
pub fn throw(env: &JNIEnv, description: &str) {
    // We cannot throw exception from this function, so errors should be written in log instead.
    let exception = match env.find_class("java/lang/RuntimeException") {
        Ok(val) => val,
        Err(e) => {
            error!("Unable to find 'RuntimeException' class: {}",
                   e.description());
            return;
        }
    };
    match env.throw_new(exception, description) {
        Ok(_) => {}
        Err(e) => {
            error!("Unable to find 'RuntimeException' class: {}",
                   e.description());
        }
    }
}

// Tries to get meaningful description from panic-error.
fn any_to_string(any: &Box<Any + Send>) -> String {
    // TODO: jni::errors::Error?
    // TODO: Handle more types?
    if let Some(error) = any.downcast_ref::<Box<Error>>() {
        error.description().to_string()
    } else if let Some(s) = any.downcast_ref::<&str>() {
        s.to_string()
    } else {
        "Unknown error occured".to_string()
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn cast_simple_object() {
        static VALUE: i32 = 0;

        let mut object = Box::new(VALUE);
        let ptr = &mut *object as *mut i32;
        let casted = cast_object::<i32>(ptr as jlong);
        assert_eq!(casted, &VALUE);
    }

    #[test]
    #[should_panic(expected = "assertion failed: object != 0")]
    fn cast_zero_object() {
        let _ = cast_object::<i32>(0);
    }
}
