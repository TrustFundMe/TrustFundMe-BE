const fs = require('fs');
fetch("http://127.0.0.1:8082/api/feed-posts/by-target?targetId=2&targetType=EXPENDITURE").then(async r => {
  const text = await r.text();
  fs.writeFileSync('err.txt', text);
}).catch(console.error);
