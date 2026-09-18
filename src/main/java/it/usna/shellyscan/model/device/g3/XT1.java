package it.usna.shellyscan.model.device.g3;

/**
 * XT1 PbS base model
 */
public interface XT1 {
	static final String ID = "XT1";
	static final String MODEL = "S3XT-0S";
}

// model S3XT-0S (LinkedGo ST1820)
// Feature implemented as virtual components; e.g.:
// http://192.168.1.xxx/rpc/Number.GetConfig?id=202 - http://192.168.1.xxx/rpc/Number.GetStatus?id=202
// http://192.168.1.xxx/rpc/Shelly.GetComponents?keys=["boolean:202","number:200","number:201","number:202"]
// http://192.168.1.xxx/rpc/Number.Set?id=202&value=30