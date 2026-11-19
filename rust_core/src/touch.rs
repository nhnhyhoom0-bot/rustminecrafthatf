use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct TouchControlButton {
    pub id: String,
    pub label: String,
    pub keycode: String,
    pub x_percent: f32,
    pub y_percent: f32,
    pub scale: f32,
    pub opacity: f32,
}

pub fn clamp_button(mut btn: TouchControlButton) -> TouchControlButton {
    btn.x_percent = btn.x_percent.clamp(0.05, 0.95);
    btn.y_percent = btn.y_percent.clamp(0.08, 0.92);
    btn.scale = btn.scale.clamp(0.6, 1.8);
    btn.opacity = btn.opacity.clamp(0.2, 1.0);
    btn
}
