var exec = require('cordova/exec');

exports.buildImage = function(args, success, error) {
    args = JSON.parse(args);
    printContent = args.printContent;
    printTemplate = args.printTemplate;
    exec(success, error, "posprintermanager", "buildImage", [
        [printContent], printTemplate
    ]);
};