# CMS API Reference

All CMS endpoints are under the `/cms` prefix. Authentication is required unless noted otherwise.

## Site Settings

### GET /cms/settings
Returns all site settings as a key-value map.

**Response:**
```json
{
  "site_name": "Dashboard",
  "site_logo": "/logo.svg",
  "site_logo_width": "100"
}
```

### PUT /cms/settings
Bulk upsert settings. Requires Admin role.

**Request Body:**
```json
{
  "site_name": "My Site",
  "site_logo": "/uploads/2026/04/abc123.png",
  "site_logo_width": "120"
}
```

**Response:** Updated settings map (same format as GET).

### GET /cms/settings/{key}
Get a single setting value.

**Response:**
```json
{ "key": "site_name", "value": "Dashboard" }
```

---

## Pages

### GET /cms/pages/{slug}/render
**Public** — No authentication required.

Returns a fully assembled page with template structure merged with content values.

**Response:**
```json
{
  "id": 1,
  "title": "About Us",
  "slug": "about-us",
  "status": "published",
  "metaTitle": "About Us - Dashboard",
  "metaDescription": "Learn more about us",
  "templateName": "Basic Page",
  "sections": [
    {
      "key": "main",
      "label": "Main Content",
      "blocks": [
        {
          "key": "title",
          "type": "text",
          "label": "Page Title",
          "required": true,
          "value": "About Us"
        },
        {
          "key": "body",
          "type": "richtext",
          "label": "Body Content",
          "required": true,
          "value": "<h2>Welcome</h2><p>This is our page.</p>"
        }
      ]
    }
  ]
}
```

### PUT /cms/pages/{id}/publish
Publish a page. Sets status to "published" and records the publish timestamp. Requires Admin role.

**Response:** Updated CmsPage entity.

### PUT /cms/pages/{id}/unpublish
Unpublish a page. Sets status to "draft" and clears publish timestamp. Requires Admin role.

**Response:** Updated CmsPage entity.

---

## Menu Reorder

### PUT /cms/menu/{menuType}/reorder
Batch update menu item order. Requires Admin role.

**Path Parameters:**
- `menuType` — `main` or `admin`

**Request Body:**
```json
[
  { "id": 1, "item_order": 1 },
  { "id": 2, "item_order": 2 },
  { "id": 3, "item_order": 3 }
]
```

**Response:**
```json
{ "status": "ok" }
```

---

## File Upload

### POST /cms/upload
Upload an image file. Requires Admin role. Uses `multipart/form-data`.

**Request:** Form field `file` with the image file.

**Allowed types:** PNG, JPEG, GIF, SVG, WebP. Max size: 10MB.

**Response:**
```json
{
  "id": 1,
  "originalName": "my-logo.png",
  "storedName": "a1b2c3d4-e5f6-7890-abcd-ef1234567890.png",
  "filePath": "/uploads/2026/04/a1b2c3d4-e5f6-7890-abcd-ef1234567890.png",
  "mimeType": "image/png",
  "fileSize": 24576,
  "createdAt": "2026-04-05T10:30:00.000Z",
  "createdBy": "admin@localhost"
}
```

### DELETE /cms/upload/{id}
Delete an uploaded file (from disk and database). Requires Admin role.

**Response:**
```json
{ "status": "deleted" }
```

---

## Dynamic CRUD for CMS Tables

The existing `/{table}/{id}` dynamic CRUD endpoints also work for all CMS tables:

| Table | Purpose |
|-------|---------|
| `site_setting` | Key-value site settings |
| `cms_template` | Page templates with JSON structure |
| `cms_page` | Content pages |
| `cms_file` | Uploaded file registry |
| `main_menu_item` | Main navigation menu items |
| `admin_menu_item` | Admin section menu items |

### Template Structure JSON Schema

```json
{
  "sections": [
    {
      "key": "section-key",
      "label": "Section Label",
      "blocks": [
        {
          "key": "block-key",
          "type": "text|richtext|image|html",
          "label": "Block Label",
          "required": true
        }
      ]
    }
  ]
}
```

### Page Content JSON Schema

```json
{
  "section-key.block-key": "content value",
  "hero.title": "My Page Title",
  "hero.background_image": "/uploads/2026/04/image.jpg",
  "body.content": "<p>HTML content here</p>"
}
```

---

## Static Files

### GET /uploads/**
**Public** — No authentication required.

Serves uploaded files from the server's upload directory. Files are organized by date: `/uploads/YYYY/MM/filename.ext`.
