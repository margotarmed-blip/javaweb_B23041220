package service;

import base.EventListener;
import base.Subscribe;
import database.DbOps;
import events.ApiType;
import events.BlobEvent;
import java.util.Base64;
public class BlobService implements EventListener {

    @Subscribe
    public void onBlobEvent(BlobEvent ev) {
        try {
            if (ev.getType() == ApiType.UploadBlob) {
                String base64Content = (String) ev.getParams().get("content");
                byte[] byteContent = Base64.getDecoder().decode(base64Content);
                DbOps.uploadBlob(
                        (String) ev.getParams().get("hash"),
                        (String) ev.getParams().get("filename"),
                        byteContent
                );
                ev.setResp("{\"success\":true}");
            } else if (ev.getType() == ApiType.GetBlob) {
                byte[] content = DbOps.getBlobContent(
                        (String) ev.getParams().get("filename"),
                        (String) ev.getParams().get("hash")
                );
                if (content != null) {
                    ev.setResp("{\"content\":\"" + Base64.getEncoder().encodeToString(content) + "\"}");
                } else {
                    ev.setResp("{\"error\":\"Access denied or file not found\"}");
                }
            }
        } catch (Exception e) {
            ev.setResp("{\"error\":\"BlobService error: " + e.getMessage() + "\"}");
        }
    }
}