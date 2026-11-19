use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum SkinModel {
    Classic,
    Slim,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SkinValidationResult {
    pub is_valid: bool,
    pub is_legacy_format: bool,
    pub recommended_model: SkinModel,
    pub message: String,
}

pub fn validate_skin_dimensions(width: u32, height: u32) -> SkinValidationResult {
    match (width, height) {
        (64, 64) => SkinValidationResult {
            is_valid: true,
            is_legacy_format: false,
            recommended_model: SkinModel::Classic,
            message: "Valid 64x64 HD skin texture".to_string(),
        },
        (64, 32) => SkinValidationResult {
            is_valid: true,
            is_legacy_format: true,
            recommended_model: SkinModel::Classic,
            message: "Valid 64x32 legacy skin texture".to_string(),
        },
        (128, 128) => SkinValidationResult {
            is_valid: true,
            is_legacy_format: false,
            recommended_model: SkinModel::Classic,
            message: "Valid 128x128 high-res skin texture".to_string(),
        },
        _ => SkinValidationResult {
            is_valid: false,
            is_legacy_format: false,
            recommended_model: SkinModel::Classic,
            message: format!("Invalid dimensions {}x{} (expected 64x64 or 64x32)", width, height),
        },
    }
}
