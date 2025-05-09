# SPDX-License-Identifier: Siemens-ISL-1.5
# Variables used by the TF scripts

variable "kc_client_id" {
  description = "ClientID of master realm"
  type        = string
  default     = null
}

variable "kc_client_secret" {
  description = "ClientSecret of master realm"
  type        = string
  default     = null
}

variable "kc_base_url" {
  description = "Base URL of KC. e.g. http://localhost:8083"
  type        = string
  default     = null
}

variable "smtp_from" {
  description = "SMTP from email"
  type        = string
  default     = null
}

variable "smtp_username" {
  description = "SMTP login username"
  type        = string
  default     = null
}

variable "smtp_password" {
  description = "SMTP login password"
  type        = string
  default     = null
}

variable "redirect_uris" {
  description = "Valid redirect URIs for client login"
  type        = set(string)
  default     = null
}

variable "tenant" {
  description = "Azure tenant ID"
  type        = string
  default     = "common"
}

variable "azure_client_id" {
  description = "ClientID from Azure"
  type        = string
  default     = null
}

variable "azure_client_secret" {
  description = "ClientSecret from Azure"
  type        = string
  default     = null
}

variable "frontend_base_url" {
  description = "Home URL for frontend"
  type        = string
  default     = null
}

variable "dashboard_base_url" {
  description = "Home URL for grafana dashboard"
  type        = string
  default     = null
}
