use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum ModSource {
    Modrinth,
    CurseForge,
    Local,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ModInstallSpec {
    pub project_id: String,
    pub title: String,
    pub source: ModSource,
    pub loader: String,
    pub mc_version: String,
}

pub fn build_modrinth_facets(loader: Option<&str>, mc_version: Option<&str>) -> String {
    let mut facets: Vec<String> = vec![r#"["project_type:mod"]"#.to_string()];
    if let Some(l) = loader {
        if !l.is_empty() && l != "all" && l != "vanilla" {
            facets.push(format!(r#"["categories:{}"]"#, l.to_lowercase()));
        }
    }
    if let Some(v) = mc_version {
        if !v.is_empty() && v != "all" {
            facets.push(format!(r#"["versions:{}"]"#, v));
        }
    }
    format!("[{}]", facets.join(","))
}
