Name:       hello-world
Version:    1
Release:    1
Summary:    A very simple RPM package
License:    GPLv2 with exceptions

%description
This is a very simple RPM package for testing purposes.

%prep

%build
cat > hello-world.sh <<EOF
#!/usr/bin/env bash
echo Hello world
EOF

%install
mkdir -p %{buildroot}/usr/local/bin
install -m 755 hello-world.sh %{buildroot}/usr/local/bin/hello-world.sh

%files
/usr/local/bin/hello-world.sh

%changelog
