use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum AccountKind {
    Offline,
    Microsoft,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct LauncherAccount {
    pub username: String,
    pub uuid: String,
    pub kind: AccountKind,
    pub skin_server_url: Option<String>,
}

/// Generates a standard Minecraft OfflinePlayer UUIDv3 from username.
/// Matches `UUID.nameUUIDFromBytes("OfflinePlayer:<name>".getBytes(UTF_8))`.
pub fn generate_offline_uuid(username: &str) -> String {
    let input = format!("OfflinePlayer:{}", username.trim());
    let mut digest = md5::compute(input.as_bytes()).0;

    // Set version to 3 (name-based MD5)
    digest[6] = (digest[6] & 0x0f) | 0x30;
    // Set variant to IETF RFC 4122
    digest[8] = (digest[8] & 0x3f) | 0x80;

    format!(
        "{:02x}{:02x}{:02x}{:02x}-{:02x}{:02x}-{:02x}{:02x}-{:02x}{:02x}-{:02x}{:02x}{:02x}{:02x}{:02x}{:02x}",
        digest[0], digest[1], digest[2], digest[3],
        digest[4], digest[5],
        digest[6], digest[7],
        digest[8], digest[9],
        digest[10], digest[11], digest[12], digest[13], digest[14], digest[15]
    )
}
