pub mod auth;
pub mod jni_bridge;
pub mod manifest;
pub mod mods;
pub mod skins;
pub mod touch;

pub const ENGINE_VERSION: &str = env!("CARGO_PKG_VERSION");
pub const ENGINE_NAME: &str = "RustCraft Core Engine (Rust 2021 / JNI)";

pub fn build_jvm_launch_args(
    ram_mb: u32,
    username: &str,
    uuid: &str,
    mc_version: &str,
    loader: &str,
    skin_server_url: Option<&str>,
    renderer: &str,
) -> Vec<String> {
    let mut args = vec![
        format!("-Xms{}M", (ram_mb / 2).max(512)),
        format!("-Xmx{}M", ram_mb.max(1024)),
        "-XX:+UseG1GC".to_string(),
        "-XX:+ParallelRefProcEnabled".to_string(),
        "-XX:MaxGCPauseMillis=200".to_string(),
        format!("-Dorg.lwjgl.opengl.libname={}", renderer),
    ];

    if let Some(url) = skin_server_url {
        if !url.trim().is_empty() {
            args.push(format!("-Dauthlibinjector.api={}", url.trim()));
        }
    }

    args.push(format!("--username={}", username));
    args.push(format!("--uuid={}", uuid));
    args.push(format!("--version={}", mc_version));
    args.push(format!("--loader={}", loader));
    args
}
