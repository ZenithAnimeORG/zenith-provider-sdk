import { PluginManifest } from "./models.js";

export const PACKAGE_FORMAT_V1 = "zpk/v1";
export const PACKAGE_FORMAT_V2 = "zpk/v2";
export const CURRENT_PACKAGE_FORMAT = PACKAGE_FORMAT_V2;
export const CURRENT_MODULE_API_VERSION = "1.0.0";

export const MAX_PACKAGE_SIZE_BYTES = 16 * 1024 * 1024; // 16 MiB
export const MAX_ENTRYPOINT_BYTES = 5 * 1024 * 1024; // 5 MiB
export const MAX_MANIFEST_BYTES = 256 * 1024; // 256 KiB
export const MAX_SETTINGS_BYTES = 128 * 1024; // 128 KiB
export const MAX_ICON_BYTES = 256 * 1024; // 256 KiB
export const MAX_RESOURCE_BYTES = 2 * 1024 * 1024; // 2 MiB
export const MAX_TOTAL_UNCOMPRESSED_BYTES = 32 * 1024 * 1024; // 32 MiB
export const MAX_ENTRY_COUNT = 100;

export interface ValidationResult {
  readonly valid: boolean;
  readonly errors: string[];
}

export function isSafeEntryName(name: string): boolean {
  if (!name || name.trim() === "") return false;
  if (name.startsWith("/") || name.startsWith("\\")) return false;
  if (name === "." || name === "..") return false;
  if (name.includes("..")) return false;
  if (name.includes("//") || name.includes("\\\\")) return false;
  const parts = name.split(/[/\\]/);
  if (parts.some((p) => p === "." || p === ".." || p.trim() === "")) return false;
  return /^[a-zA-Z0-9_\-./]+$/.test(name);
}

export function validateManifest(manifest: any): ValidationResult {
  const errors: string[] = [];

  if (!manifest || typeof manifest !== "object") {
    return { valid: false, errors: ["Manifest must be a non-null object"] };
  }

  if (typeof manifest.id !== "string" || !/^[A-Za-z0-9._-]{1,128}$/.test(manifest.id)) {
    errors.push("Manifest 'id' must be 1-128 alphanumeric characters (including . _ -)");
  }

  if (typeof manifest.name !== "string" || manifest.name.trim() === "") {
    errors.push("Manifest 'name' must be a non-empty string");
  }

  if (typeof manifest.version !== "string" || manifest.version.trim() === "") {
    errors.push("Manifest 'version' must be a non-empty string");
  }

  if (manifest.packageFormat && manifest.packageFormat !== PACKAGE_FORMAT_V1 && manifest.packageFormat !== PACKAGE_FORMAT_V2) {
    errors.push(`Unsupported packageFormat: '${manifest.packageFormat}'. Expected '${PACKAGE_FORMAT_V2}'`);
  }

  if (!Array.isArray(manifest.capabilities) || manifest.capabilities.length === 0) {
    errors.push("Manifest 'capabilities' must be a non-empty array of ProviderCapability");
  }

  return {
    valid: errors.length === 0,
    errors,
  };
}

export function validateEnvelope(
  entries: Array<{ name: string; size: number }>,
  manifest: PluginManifest
): ValidationResult {
  const errors: string[] = [];

  if (entries.length > MAX_ENTRY_COUNT) {
    errors.push(`Archive entry count ${entries.length} exceeds maximum limit of ${MAX_ENTRY_COUNT}`);
  }

  let totalBytes = 0;
  const names = new Set<string>();

  for (const entry of entries) {
    if (names.has(entry.name)) {
      errors.push(`Duplicate entry in package: ${entry.name}`);
    }
    names.add(entry.name);

    if (!isSafeEntryName(entry.name)) {
      errors.push(`Unsafe entry path in package: ${entry.name}`);
    }

    totalBytes += entry.size;

    // Check for banned binary types in portable zpk/v2
    const lower = entry.name.toLowerCase();
    if (
      manifest.packageFormat === PACKAGE_FORMAT_V2 &&
      (lower.endsWith(".jar") ||
        lower.endsWith(".dex") ||
        lower.endsWith(".class") ||
        lower.endsWith(".so") ||
        lower.endsWith(".dll") ||
        lower.endsWith(".dylib") ||
        lower.endsWith(".exe") ||
        lower.endsWith(".apk"))
    ) {
      errors.push(`Forbidden executable binary in portable package: ${entry.name}`);
    }

    if (entry.name === "manifest.json" && entry.size > MAX_MANIFEST_BYTES) {
      errors.push(`manifest.json exceeds size limit of ${MAX_MANIFEST_BYTES} bytes`);
    }

    const entrypoint = manifest.entrypoint ?? "index.js";
    if (entry.name === entrypoint && entry.size > MAX_ENTRYPOINT_BYTES) {
      errors.push(`Entrypoint '${entrypoint}' exceeds size limit of ${MAX_ENTRYPOINT_BYTES} bytes`);
    }
  }

  if (totalBytes > MAX_TOTAL_UNCOMPRESSED_BYTES) {
    errors.push(
      `Total uncompressed bytes ${totalBytes} exceeds maximum limit of ${MAX_TOTAL_UNCOMPRESSED_BYTES}`
    );
  }

  if (!names.has("manifest.json")) {
    errors.push("Package envelope is missing required entry: manifest.json");
  }

  const entrypoint = manifest.entrypoint ?? "index.js";
  if (!names.has(entrypoint)) {
    errors.push(`Package envelope is missing declared entrypoint: ${entrypoint}`);
  }

  return {
    valid: errors.length === 0,
    errors,
  };
}
