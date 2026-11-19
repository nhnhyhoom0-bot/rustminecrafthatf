use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct LatestVersions {
    pub release: String,
    pub snapshot: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct VersionEntry {
    pub id: String,
    #[serde(rename = "type")]
    pub kind: String,
    pub url: String,
    #[serde(rename = "releaseTime")]
    pub release_time: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct VersionManifestV2 {
    pub latest: LatestVersions,
    pub versions: Vec<VersionEntry>,
}

pub fn parse_manifest(json_str: &str) -> Result<VersionManifestV2, serde_json::Error> {
    serde_json::from_str(json_str)
}

pub fn has_newer_release(current: &str, latest_release: &str) -> bool {
    !current.trim().is_empty() && current.trim() != latest_release.trim()
}
