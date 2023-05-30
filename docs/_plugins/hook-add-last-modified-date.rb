Jekyll::Hooks.register :posts, :pre_render do |post|

  # get the current post last modified time
  modification_time = File.mtime( post.path )

  # inject modification_time in post's datas.
  post.data['last-modified-date'] = modification_time

end
