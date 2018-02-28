var exec = require('cordova/exec');

exports.buildImage = function(args, success, error) {
    args = JSON.parse(args);
    printContent = args.printContent;
    printTemplate = args.printTemplate;
    printCanvas = [545, 0, 0, 15];
    exec(success, error, "posprintermanager", "buildImage", [
        [printContent], printTemplate, printCanvas
    ]);
};

exports.search = function(args, success, error) {
    args = JSON.parse(args);
    timeout = args.timeout;
    vendor = args.vendor;
    type = args.type;
    exec(success, error, "posprintermanager", "search", [
        timeout, vendor, type
    ]);
};