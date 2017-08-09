const Pageres = require('pageres');
 
const pageres = new Pageres({delay: 15})
    .src('localhost:9000', ['1024x768'], {crop: true, filename: "<%= date %> - <%= time %>"})
    .dest("images")
    .run()
    .then(() => console.log('done'));