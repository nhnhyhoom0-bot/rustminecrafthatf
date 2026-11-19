use jni::objects::{JClass, JString};
use jni::sys::{jboolean, jint, jstring, JNI_FALSE, JNI_TRUE};
use jni::JNIEnv;

use crate::auth::generate_offline_uuid;
use crate::skins::validate_skin_dimensions;
use crate::{build_jvm_launch_args, ENGINE_NAME, ENGINE_VERSION};

#[no_mangle]
pub extern "system" fn Java_com_example_core_RustCoreBridge_nativeGetEngineBanner(
    env: JNIEnv,
    _class: JClass,
) -> jstring {
    let banner = format!("{} v{}", ENGINE_NAME, ENGINE_VERSION);
    env.new_string(banner)
        .map(|s| s.into_raw())
        .unwrap_or(std::ptr::null_mut())
}

#[no_mangle]
pub extern "system" fn Java_com_example_core_RustCoreBridge_nativeGenerateOfflineUuid(
    mut env: JNIEnv,
    _class: JClass,
    username: JString,
) -> jstring {
    let name: String = env
        .get_string(&username)
        .map(|s| s.into())
        .unwrap_or_else(|_| "Player".to_string());
    let uuid = generate_offline_uuid(&name);
    env.new_string(uuid)
        .map(|s| s.into_raw())
        .unwrap_or(std::ptr::null_mut())
}

#[no_mangle]
pub extern "system" fn Java_com_example_core_RustCoreBridge_nativeValidateSkinDimensions(
    _env: JNIEnv,
    _class: JClass,
    width: jint,
    height: jint,
) -> jboolean {
    if width < 0 || height < 0 {
        return JNI_FALSE;
    }
    let res = validate_skin_dimensions(width as u32, height as u32);
    if res.is_valid {
        JNI_TRUE
    } else {
        JNI_FALSE
    }
}

#[no_mangle]
pub extern "system" fn Java_com_example_core_RustCoreBridge_nativeBuildJvmArgs(
    mut env: JNIEnv,
    _class: JClass,
    ram_mb: jint,
    username: JString,
    uuid: JString,
    mc_version: JString,
    loader: JString,
    skin_server: JString,
    renderer: JString,
) -> jstring {
    let get = |env: &mut JNIEnv, s: &JString, fallback: &str| -> String {
        env.get_string(s)
            .map(|js| js.into())
            .unwrap_or_else(|_| fallback.to_string())
    };
    let u_name = get(&mut env, &username, "Steve");
    let u_uuid = get(&mut env, &uuid, "00000000-0000-3000-8000-000000000000");
    let ver = get(&mut env, &mc_version, "1.21.4");
    let ldr = get(&mut env, &loader, "Fabric");
    let srv = get(&mut env, &skin_server, "");
    let rnd = get(&mut env, &renderer, "gl4es");

    let args = build_jvm_launch_args(
        ram_mb.max(512) as u32,
        &u_name,
        &u_uuid,
        &ver,
        &ldr,
        if srv.is_empty() { None } else { Some(srv.as_str()) },
        &rnd,
    );
    env.new_string(args.join(" "))
        .map(|s| s.into_raw())
        .unwrap_or(std::ptr::null_mut())
}
